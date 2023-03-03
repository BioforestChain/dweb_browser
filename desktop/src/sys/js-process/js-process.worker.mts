/// <reference lib="webworker"/>
/// 该文件是给 js-worker 用的，worker 中是纯粹的一个runtime，没有复杂的 import 功能，所以这里要极力克制使用外部包。
/// import 功能需要 chrome-80 才支持。我们明年再支持 import 吧，在此之前只能用 bundle 方案来解决问题

import { MessagePortIpc } from "../../core/ipc-web/MessagePortIpc.cjs";
import { IPC_ROLE } from "../../core/ipc/index.cjs";
import { fetchExtends } from "../../helper/$makeFetchExtends.cjs";
import { $readRequestAsIpcRequest } from "../../helper/$readRequestAsIpcRequest.cjs";
import { normalizeFetchArgs } from "../../helper/normalizeFetchArgs.cjs";
import type {
  $IpcSupportProtocols,
  $MicroModule,
  $MMID,
} from "../../helper/types.cjs";
import { updateUrlOrigin } from "../../helper/urlHelper.cjs";
import type { $RunMainConfig } from "./assets/js-process.web.mjs";

import * as ipc from "../../core/ipc/index.cjs";
import * as http from "../http-server/$createHttpDwebServer.cjs";

class Metadata {
  constructor(private source: URLSearchParams) {}
  requiredString(key: string) {
    const val = this.optionalString(key);
    if (val === undefined) {
      throw new Error(`no found (string) ${key}`);
    }
    return val;
  }
  optionalString(key: string) {
    const val = this.source.get(key);
    if (val === null) {
      return;
    }
    return val;
  }
  requiredBoolean(key: string) {
    const val = this.optionalBoolean(key);
    if (val === undefined) {
      throw new Error(`no found (boolean) ${key}`);
    }
    return val;
  }
  optionalBoolean(key: string) {
    const val = this.optionalString(key);
    if (val === null) {
      return;
    }
    return val === "true";
  }
  stringArray(key: string) {
    return this.source.getAll(key);
  }
}

const metadata = new Metadata(new URL(import.meta.url).searchParams);

const js_process_ipc_support_protocols = (() => {
  const protocols = metadata.stringArray("ipc-support-protocols");
  return {
    raw: protocols.includes("raw"),
    message_pack: protocols.includes("message_pack"),
    protobuf: protocols.includes("protobuf"),
  } satisfies $IpcSupportProtocols;
})();

/// 这个文件是给所有的 js-worker 用的，所以会重写全局的 fetch 函数，思路与 dns 模块一致
/// 如果是在原生的系统中，不需要重写fetch函数，因为底层那边可以直接捕捉 fetch
/// 虽然 nwjs 可以通过 chrome.webRequest 来捕捉请求，但没法自定义相应内容
/// 所以这里的方案还是对 fetch 进行重写
/// 拦截到的 ipc-message 通过 postMessage 转发到 html 层，再有 html 层

/**
 * 这个是虚假的 $MicroModule，这里只是一个影子，指代 native 那边的 micro_module
 */
export class JsProcessMicroModule implements $MicroModule {
  readonly ipc_support_protocols = js_process_ipc_support_protocols;
  constructor(
    readonly mmid: $MMID,
    readonly host: String,
    readonly meta: Metadata,
    private nativeFetchPort: MessagePort
  ) {}
  readonly fetchIpc = new MessagePortIpc(
    this.nativeFetchPort,
    this,
    IPC_ROLE.SERVER
  );

  private async _nativeFetch(
    url: RequestInfo | URL,
    init?: RequestInit
  ): Promise<Response> {
    const args = normalizeFetchArgs(url, init);
    const { parsed_url } = args;
    /// 进入特殊的解析模式
    if (
      parsed_url.protocol === "file:" &&
      (parsed_url.hostname === "" || parsed_url.hostname.endsWith(".dweb"))
    ) {
      const ipc_req_init = await $readRequestAsIpcRequest(args.request_init);
      const ipc_response = await this.fetchIpc.request(
        parsed_url.href,
        ipc_req_init
      );
      return ipc_response.toResponse(parsed_url.href);
    }
    return fetch(args.parsed_url, args.request_init);
  }
  nativeFetch(url: RequestInfo | URL, init?: RequestInit) {
    return Object.assign(this._nativeFetch(url, init), fetchExtends);
  }
}

/// 消息通道构造器
const waitFetchPort = () => {
  return new Promise<MessagePort>((resolve) => {
    self.addEventListener("message", (event) => {
      const data = event.data as any[];
      if (Array.isArray(event.data) === false) {
        return;
      }
      /// 这是来自 原生接口 WebMessageChannel 创建出来的通道
      /// 由 web 主线程代理传递过来
      if (data[0] === "fetch-ipc-channel") {
        resolve(data[1]);
      }
    });
  });
};

/**
 * 安装上下文
 */
export const installEnv = async (mmid: $MMID, host: String) => {
  const jsProcess = new JsProcessMicroModule(
    mmid,
    host,
    metadata,
    await waitFetchPort()
  );

  Object.assign(globalThis, {
    jsProcess,
    JsProcessMicroModule,
    http,
    ipc,
  });
  /// 安装完成，告知外部
  self.postMessage(["env-ready"]);
  return jsProcess;
};

self.addEventListener("message", async (event) => {
  const data = event.data as any[];
  if (Array.isArray(event.data) === false) {
    return;
  }
  if (data[0] === "run-main") {
    const config = data[1] as $RunMainConfig;
    const main_parsed_url = updateUrlOrigin(
      config.main_url,
      `http://${jsProcess.host}`
    );
    const location = {
      hash: main_parsed_url.hash,
      host: main_parsed_url.host,
      hostname: main_parsed_url.hostname,
      href: main_parsed_url.href,
      origin: main_parsed_url.origin,
      pathname: main_parsed_url.pathname,
      port: main_parsed_url.port,
      protocol: main_parsed_url.protocol,
      search: main_parsed_url.search,
      toString() {
        return main_parsed_url.href;
      },
    };
    Object.setPrototypeOf(location, WorkerLocation.prototype);
    Object.freeze(location);

    Object.defineProperty(self, "location", {
      value: location,
      configurable: false,
      enumerable: false,
      writable: false,
    });

    await import(config.main_url);
  }
});

installEnv(
  metadata.requiredString("mmid") as $MMID,
  metadata.requiredString("host")
);
