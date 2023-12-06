/// <reference lib="webworker"/>
/// 该文件是给 js-worker 用的，worker 中是纯粹的一个runtime，没有复杂的 import 功能，所以这里要极力克制使用外部包。
/// import 功能需要 chrome-80 才支持。我们明年再支持 import 吧，在此之前只能用 bundle 方案来解决问题
import type { $DWEB_DEEPLINK, $IpcSupportProtocols, $MicroModule, $MMID } from "@dweb-browser/desktop/core/types.ts";
import type { $RunMainConfig } from "../main/index.module.ts";

import { $normalizeRequestInitAsIpcRequestArgs } from "@dweb-browser/desktop/core/helper/ipcRequestHelper.ts";
import { $Callback, createSignal } from "@dweb-browser/desktop/helper/createSignal.ts";
import { fetchExtends } from "@dweb-browser/desktop/helper/fetchExtends/index.ts";
import { mapHelper } from "@dweb-browser/desktop/helper/mapHelper.ts";
import { normalizeFetchArgs } from "@dweb-browser/desktop/helper/normalizeFetchArgs.ts";
import { PromiseOut } from "@dweb-browser/desktop/helper/PromiseOut.ts";
import { updateUrlOrigin } from "@dweb-browser/desktop/helper/urlHelper.ts";
export type { fetchExtends } from "@dweb-browser/desktop/helper/fetchExtends/index.ts";

import * as core from "./std-dweb-core.ts";
import * as http from "./std-dweb-http.ts";

import {
  $OnFetch,
  $OnIpcEventMessage,
  $OnIpcRequestMessage,
  createFetchHandler,
  Ipc,
  IPC_ROLE,
  IpcEvent,
  MessagePortIpc,
  MWEBVIEW_LIFECYCLE_EVENT,
} from "./std-dweb-core.ts";

declare global {
  type JsProcessMicroModuleContructor = JsProcessMicroModule;
  const JsProcessMicroModule: new (mmid: $MMID) => JsProcessMicroModuleContructor;

  interface DWebCore {
    jsProcess: JsProcessMicroModuleContructor;
    core: typeof core;
    ipc: typeof core;
    http: typeof http;
    versions: Record<string, string>;
    version: number;
    patch: number;
  }
  interface WorkerNavigator {
    readonly dweb: DWebCore;
  }
  interface Navigator {
    readonly dweb: DWebCore;
  }
}
const workerGlobal = self as DedicatedWorkerGlobalScope;

// import * as helper_createSignal from "../../helper/createSignal.ts";
// import * as helper_JsonlinesStream from "../../helper/stream/JsonlinesStream.ts";
// import * as helper_PromiseOut from "../../helper/PromiseOut.ts";
// import * as helper_readableStreamHelper from "../../helper/stream/readableStreamHelper.ts";

export class Metadata<T extends $Metadata = $Metadata> {
  constructor(readonly data: T, readonly env: Record<string, string>) {}
  envString(key: string) {
    const val = this.envStringOrNull(key);
    if (val == null) {
      throw new Error(`no found (string) ${key}`);
    }
    return val;
  }
  envStringOrNull(key: string) {
    const val = this.env[key];
    if (val == null) {
      return;
    }
    return val;
  }
  envBoolean(key: string) {
    const val = this.envBooleanOrNull(key);
    if (val == null) {
      throw new Error(`no found (boolean) ${key}`);
    }
    return val;
  }
  envBooleanOrNull(key: string) {
    const val = this.envStringOrNull(key);
    if (val == null) {
      return;
    }
    return val === "true";
  }
}

type $Metadata = {
  mmid: $MMID;
};

// const js_process_ipc_support_protocols =

/// 这个文件是给所有的 js-worker 用的，所以会重写全局的 fetch 函数，思路与 dns 模块一致
/// 如果是在原生的系统中，不需要重写fetch函数，因为底层那边可以直接捕捉 fetch
/// 虽然 nwjs 可以通过 chrome.webRequest 来捕捉请求，但没法自定义相应内容
/// 所以这里的方案还是对 fetch 进行重写
/// 拦截到的 ipc-message 通过 postMessage 转发到 html 层，再有 html 层

/**
 * 这个是虚假的 $MicroModule，这里只是一个影子，指代 native 那边的 micro_module
 */
export class JsProcessMicroModule implements $MicroModule {
  readonly ipc_support_protocols = (() => {
    const protocols = this.meta.envStringOrNull("ipc-support-protocols")?.split(/[\s\,]+/) ?? [];
    return {
      raw: protocols.includes("raw"),
      cbor: protocols.includes("cbor"),
      protobuf: protocols.includes("protobuf"),
    } satisfies $IpcSupportProtocols;
  })();
  readonly mmid: $MMID;
  readonly name: string;
  readonly host: string;
  readonly dweb_deeplinks: $DWEB_DEEPLINK[] = [];
  readonly categories: $MicroModule["categories"] = [];

  constructor(readonly meta: Metadata, private nativeFetchPort: MessagePort) {
    const _beConnect = (event: MessageEvent) => {
      const data = event.data;
      if (Array.isArray(data) === false) {
        return;
      }
      if (data[0] === "ipc-connect") {
        const mmid = data[1];
        const port = event.ports[0];
        const env = JSON.parse(data[2] ?? "{}");
        const protocols = env["ipc-support-protocols"] ?? "";
        const ipc_support_protocols = {
          raw: protocols.includes("raw"),
          cbor: protocols.includes("cbor"),
          protobuf: protocols.includes("protobuf"),
        } satisfies $IpcSupportProtocols;
        let rote = IPC_ROLE.CLIENT as IPC_ROLE;
        const port_po = mapHelper.getOrPut(this._ipcConnectsMap, mmid, () => {
          rote = IPC_ROLE.SERVER;
          return new PromiseOut<Ipc>();
        });
        const ipc = new MessagePortIpc(
          port,
          {
            mmid,
            ipc_support_protocols,
            dweb_deeplinks: [],
            categories: [],
            name: this.name,
          },
          rote
        );
        port_po.resolve(ipc);
        if (typeof navigator === "object" && navigator.locks) {
          ipc.onEvent((event) => {
            try {
              if (event.name === "web-message-port-live") {
                // console.warn(self.name, ipc.remote.mmid, "web-message-port living", event.text);
                void navigator.locks.request(event.text, () => {
                  // console.warn(self.name, ipc.remote.mmid, "web-message-port ipc closed");
                  ipc.close();
                });
              }
            } catch (e) {
              console.error("locks-2", e);
            }
          });
          ipc.ready().then(() => {
            const liveId = "live-" + Date.now() + Math.random() + "-for-" + ipc.remote.mmid;
            try {
              void navigator.locks.request(liveId, () => {
                // console.warn(self.name, "web-message-port live start", liveId);
                return new Promise(() => {}); /// 永远不释放
              });
              ipc.postMessage(IpcEvent.fromText("web-message-port-live", liveId));
            } catch (e) {
              console.error("locks-1", e);
            }
          });
        }

        workerGlobal.postMessage(["ipc-connect-ready", mmid]);
        /// 不论是连接方，还是被连接方，都需要触发事件
        this.beConnect(ipc);
        /// 分发绑定的事件
        ipc.onRequest((ipcRequest, ipc) => this._onRequestSignal.emit(ipcRequest, ipc));
        ipc.onEvent((ipcEvent, ipc) => {
          if (ipcEvent.name === MWEBVIEW_LIFECYCLE_EVENT.Activity) {
            return this._activitySignal.emit(ipcEvent, ipc);
          }
          if (ipcEvent.name === MWEBVIEW_LIFECYCLE_EVENT.Renderer) {
            return this._rendererSignal.emit(ipcEvent, ipc);
          }
          if (ipcEvent.name === MWEBVIEW_LIFECYCLE_EVENT.Close) {
            return this._onCloseSignal.emit(ipcEvent, ipc);
          }
        });
      } else if (data[0] === "ipc-connect-fail") {
        // TODO  这里希望能携带req_id 针对请求直接给出错误，而不是没有错误响应
        const mmid = data[1];
        const reason = data[2];
        this._ipcConnectsMap.get(mmid)?.reject(reason);
      }
    };
    workerGlobal.addEventListener("message", _beConnect);

    this.mmid = meta.data.mmid;
    this.name = `js process of ${this.mmid}`;
    this.host = this.meta.envString("host");
    this.fetchIpc = new MessagePortIpc(this.nativeFetchPort, this, IPC_ROLE.SERVER);
    this.fetchIpc.onEvent((ipcEvent) => {
      if (ipcEvent.name === "dns/connect/done") {
        const { connect, result } = JSON.parse(ipcEvent.data);
        const task = this._ipcConnectsMap.get(connect);
        if (task.is_resolved === false) {
          const resultTask = this._ipcConnectsMap.get(result);
          if (resultTask !== task) {
            task.resolve(resultTask.promise);
          }
        }
      }
    });
  }

  /// 这个通道只能用于基础的通讯
  readonly fetchIpc: MessagePortIpc;

  private async _nativeFetch(url: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const args = normalizeFetchArgs(url, init);
    const hostName = args.parsed_url.hostname;
    if (!(hostName.endsWith(".dweb") && args.parsed_url.protocol === "file:")) {
      const ipc_response = await this._nativeRequest(args.parsed_url, args.request_init);
      return ipc_response.toResponse(args.parsed_url.href);
    }
    const ipc = await this.connect(hostName as $MMID);
    const ipc_req_init = await $normalizeRequestInitAsIpcRequestArgs(args.request_init);
    const ipc_response = await ipc.request(args.parsed_url.href, ipc_req_init);
    return ipc_response.toResponse(args.parsed_url.href);
  }

  /**
   * 模拟fetch的返回值
   * 这里的做fetch的时候需要先connect
   */
  nativeFetch(url: RequestInfo | URL, init?: RequestInit) {
    return Object.assign(this._nativeFetch(url, init), fetchExtends);
  }

  private async _nativeRequest(parsed_url: URL, request_init: RequestInit) {
    const ipc_req_init = await $normalizeRequestInitAsIpcRequestArgs(request_init);
    return await this.fetchIpc.request(parsed_url.href, ipc_req_init);
  }

  /** 同 ipc.request，只不过使用 fetch 接口的输入参数 */
  nativeRequest(url: RequestInfo | URL, init?: RequestInit) {
    const args = normalizeFetchArgs(url, init);
    return this._nativeRequest(args.parsed_url, args.request_init);
  }

  /**重启 */
  restart() {
    this.fetchIpc.postMessage(IpcEvent.fromText("restart", "")); // 发送指令
  }
  // 外部request信号
  private _onRequestSignal = createSignal<$OnIpcRequestMessage>(false);
  // 应用激活信号
  private _activitySignal = createSignal<$OnIpcEventMessage>(false);
  onActivity(cb: $OnIpcEventMessage) {
    return this._activitySignal.listen(cb);
  }
  // 窗口激活信号
  private _rendererSignal = createSignal<$OnIpcEventMessage>(false);
  onRenderer(cb: $OnIpcEventMessage) {
    return this._rendererSignal.listen(cb);
  }

  onRequest(request: $OnIpcRequestMessage) {
    return this._onRequestSignal.listen(request);
  }

  onFetch(...handlers: $OnFetch[]) {
    const onRequest = createFetchHandler(handlers);
    return onRequest.extendsTo(this.onRequest(onRequest));
  }

  // app关闭信号
  private _onCloseSignal = createSignal<$OnIpcEventMessage>(false);
  onClose(cb: $OnIpcEventMessage) {
    return this._onCloseSignal.listen(cb);
  }

  private _ipcConnectsMap = new Map<$MMID, PromiseOut<Ipc>>();
  async connect(mmid: $MMID) {
    const ipc = await mapHelper.getOrPut(this._ipcConnectsMap, mmid, () => {
      const ipc_po = new PromiseOut<Ipc>();
      // 发送指令
      this.fetchIpc.postMessage(
        IpcEvent.fromText(
          "dns/connect",
          JSON.stringify({
            mmid,
            /// 要求使用 ready 协议
            sub_protocols: ["ready"],
          })
        )
      );
      ipc_po.onSuccess((ipc) => {
        ipc.onClose(() => {
          this._ipcConnectsMap.delete(ipc.remote.mmid);
        });
      });
      return ipc_po;
    }).promise;
    /// 等待对方响应ready协议
    await ipc.ready();
    return ipc;
  }

  private _ipcSet = new Set<Ipc>();
  addToIpcSet(ipc: Ipc) {
    this._ipcSet.add(ipc);
    ipc.onClose(() => {
      this._ipcSet.delete(ipc);
    });
    void ipc.ready();
  }

  private _connectSignal = createSignal<$Callback<[Ipc]>>(false);
  beConnect(ipc: Ipc) {
    this.addToIpcSet(ipc);
    this._connectSignal.emit(ipc);
  }
  onConnect(cb: $Callback<[Ipc]>) {
    return this._connectSignal.listen(cb);
  }
}

/// 消息通道构造器
const waitFetchPort = () => {
  return new Promise<MessagePort>((resolve) => {
    workerGlobal.addEventListener("message", function onFetchIpcChannel(event) {
      const data = event.data;
      if (Array.isArray(event.data) === false) {
        return;
      }
      /// 这是来自 原生接口 WebMessageChannel 创建出来的通道
      /// 由 web 主线程代理传递过来
      if (data[0] === "fetch-ipc-channel") {
        resolve(data[1]);
        workerGlobal.removeEventListener("message", onFetchIpcChannel);
      }
    });
  });
};

/**
 * 安装上下文
 */
export const installEnv = async (metadata: Metadata, versions: Record<string, string>) => {
  const jsProcess = new JsProcessMicroModule(metadata, await waitFetchPort());
  const [version, patch] = versions.jsMicroModule.split(".").map((v) => parseInt(v));
  const dweb = {
    jsProcess,
    core,
    ipc: core,
    http,
    versions,
    version,
    patch,
  } satisfies DWebCore;
  // Object.assign(globalThis, dweb);
  Object.assign(navigator, { dweb });
  /// 安装完成，告知外部
  workerGlobal.postMessage(["env-ready"]);

  workerGlobal.addEventListener("message", async function runMain(event) {
    const data = event.data;
    if (Array.isArray(event.data) === false) {
      return;
    }
    if (data[0] === "run-main") {
      const config = data[1] as $RunMainConfig;
      const main_parsed_url = updateUrlOrigin(
        config.main_url,
        `${self.location.href.startsWith("blob:https:") ? "https" : "http"}://${jsProcess.host}`
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

      Object.defineProperty(workerGlobal, "location", {
        value: location,
        configurable: false,
        enumerable: false,
        writable: false,
      });

      await import(config.main_url);
      workerGlobal.removeEventListener("message", runMain);
    }
  });
  return jsProcess;
};
