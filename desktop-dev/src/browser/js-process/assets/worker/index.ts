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
  $messageToIpcMessage,
  $OnFetch,
  $OnIpcEventMessage,
  $OnIpcRequestMessage,
  createFetchHandler,
  Ipc,
  IPC_HANDLE_EVENT,
  IPC_ROLE,
  IpcEvent,
  IpcRequest,
  MessagePortIpc,
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
          const ipc_po = new PromiseOut<MessagePortIpc>();
          ipc_po.onSuccess((ipc) => {
            ipc.onClose(() => {
              this._ipcConnectsMap.delete(mmid);
            });
          });
          return ipc_po;
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
          this.afterIpcReady(ipc).then(() => {
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
          // 激活
          if (ipcEvent.name === IPC_HANDLE_EVENT.Activity) {
            return this._activitySignal.emit(ipcEvent, ipc);
          }
          // 渲染
          if (ipcEvent.name === IPC_HANDLE_EVENT.Renderer) {
            return this._rendererSignal.emit(ipcEvent, ipc);
          }
          if (ipcEvent.name === IPC_HANDLE_EVENT.RendererDestroy) {
            return this._rendererDestroySignal.emit(ipcEvent, ipc);
          }
          // quick action
          if (ipcEvent.name === IPC_HANDLE_EVENT.Shortcut) {
            return this._shortcutSignal.emit(ipcEvent, ipc);
          }

          // 关闭
          if (ipcEvent.name === IPC_HANDLE_EVENT.Close) {
            return this._onCloseSignal.emit(ipcEvent, ipc);
          }
        });
        ipc.onError((error) => {
          console.log("js-process onError=>", error);
          this._ipcConnectsMap.get(mmid)?.reject(error);
        });
      }
    };
    workerGlobal.addEventListener("message", _beConnect);

    this.mmid = meta.data.mmid;
    this.name = `js process of ${this.mmid}`;
    this.host = this.meta.envString("host");
    this.fetchIpc = new MessagePortIpc(this.nativeFetchPort, this, IPC_ROLE.SERVER);
    this.fetchIpc.onEvent(async (ipcEvent) => {
      if (ipcEvent.name === "dns/connect/done" && typeof ipcEvent.data === "string") {
        const { connect, result } = JSON.parse(ipcEvent.data);
        /// 这里之所以 connect 和 result 存在不一致的情况，是因为 subprotocol 的存在
        const task = this._ipcConnectsMap.get(connect);
        if (task && task.is_resolved === false) {
          const resultTask = this._ipcConnectsMap.get(result);
          if (resultTask && resultTask !== task) {
            task.resolve(resultTask.promise);
          }
        }
      } else if (ipcEvent.name.startsWith("forward/")) {
        const [_, action, mmid] = ipcEvent.name.split("/");
        const ipc = await this.connect(mmid as $MMID);
        if (action === "request") {
          const response = await ipc.request(
            $messageToIpcMessage(JSON.parse(ipcEvent.data as string), ipc) as IpcRequest
          );
          this.fetchIpc.postMessage(
            IpcEvent.fromText(`forward/response/${mmid}`, JSON.stringify(response.ipcResMessage()))
          );
        } else if (action === "close") {
          ipc.close();
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
    let ipc_response = await ipc.request(args.parsed_url.href, ipc_req_init);
    if (ipc_response.statusCode === 401) {
      /// 尝试进行授权请求
      try {
        const permissions = await ipc_response.body.text();
        if (await this.requestDwebPermissions(permissions)) {
          /// 如果授权完全成功，那么重新进行请求
          ipc_response = await ipc.request(args.parsed_url.href, ipc_req_init);
        }
      } catch (e) {
        console.error("fail to request permission:", e);
      }
    }
    return ipc_response.toResponse(args.parsed_url.href);
  }

  async requestDwebPermissions(permissions: string) {
    const res = await (
      await this.nativeFetch(
        new URL(`file://permission.std.dweb/request?permissions=${encodeURIComponent(permissions)}`)
      )
    ).text();
    const requestPermissionResult: Record<string, string> = JSON.parse(res);
    return Object.values(requestPermissionResult).every((status) => status === "granted");
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
  private _shortcutSignal = createSignal<$OnIpcEventMessage>(false);
  onShortcut(cb: $OnIpcEventMessage) {
    return this._shortcutSignal.listen(cb);
  }
  onActivity(cb: $OnIpcEventMessage) {
    return this._activitySignal.listen(cb);
  }
  // 窗口激活信号
  private _rendererSignal = createSignal<$OnIpcEventMessage>(false);
  onRenderer(cb: $OnIpcEventMessage) {
    return this._rendererSignal.listen(cb);
  }
  private _rendererDestroySignal = createSignal<$OnIpcEventMessage>(false);
  onRendererDestroy(cb: $OnIpcEventMessage) {
    return this._rendererDestroySignal.listen(cb);
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

  private _ipcConnectsMap = new Map<$MMID, PromiseOut<MessagePortIpc>>();
  async connect(mmid: $MMID) {
    const ipc = await mapHelper.getOrPut(this._ipcConnectsMap, mmid, () => {
      const ipc_po = new PromiseOut<MessagePortIpc>();
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
          this._ipcConnectsMap.delete(mmid);
        });
      });
      return ipc_po;
    }).promise;
    /// 等待对方响应ready协议
    await this.afterIpcReady(ipc);
    return ipc;
  }

  private _ipcSet = new Set<Ipc>();
  addToIpcSet(ipc: Ipc) {
    this._ipcSet.add(ipc);
    ipc.onClose(() => {
      this._ipcSet.delete(ipc);
    });
    void this.afterIpcReady(ipc);
  }

  private _appReady = new PromiseOut<void>();
  private async afterIpcReady(ipc: Ipc) {
    await this._appReady.promise;
    await ipc.ready();
  }

  ready() {
    this._appReady.resolve();
  }

  private _connectSignal = createSignal<$Callback<[Ipc]>>(false);
  beConnect(ipc: Ipc) {
    this.addToIpcSet(ipc);
    this._connectSignal.emit(ipc);
  }
  onConnect(cb: $Callback<[Ipc]>) {
    return this._connectSignal.listen(cb);
  }
  // 提供一个关闭通信的功能
  close(reson?: any) {
    this._ipcConnectsMap.forEach(async (ipc) => {
      ipc.promise.then((res) => {
        res.postMessage(IpcEvent.fromText("close", reson));
        res.close();
      });
    });
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

const originalFetch = fetch;

const httpFetch = (input: RequestInfo | URL, init?: RequestInit) => {
  let inputUrl = "https://http.std.dweb/fetch";
  const searchParams = new URLSearchParams();
  if (input instanceof Request) {
    searchParams.set("url", input.url);
    searchParams.set("credentials", input.credentials);
  } else if (typeof input === "string") {
    searchParams.set("url", input);
  } else if (input instanceof URL) {
    searchParams.set("url", input.href);
  }

  inputUrl += `?${searchParams.toString()}`;
  return originalFetch(inputUrl, init);
};

class DwebXMLHttpRequest extends XMLHttpRequest {
  #inputUrl = "https://http.std.dweb/fetch";

  override open(method: string, url: string | URL): void;
  override open(
    method: string,
    url: string | URL,
    async: boolean,
    username?: string | null | undefined,
    password?: string | null | undefined
  ): void;
  override open(method: string, url: string | URL): void;
  override open(
    method: string,
    url: string | URL,
    async: boolean,
    username?: string | null | undefined,
    password?: string | null | undefined
  ): void;
  override open(method: string, url: string | URL): void;
  override open(
    method: string,
    url: string | URL,
    async: boolean,
    username?: string | null | undefined,
    password?: string | null | undefined
  ): void;
  override open(method: unknown, url: unknown, async?: unknown, username?: unknown, password?: unknown): void {
    let input: URL;
    if (typeof url === "string") {
      input = new URL(url);
    } else if (url instanceof URL) {
      input = url;
    }
    this.#inputUrl += `?url=${input!.href}`;

    super.open(
      method as string,
      this.#inputUrl,
      async ? true : false,
      username ? (username as string) : null,
      password ? (password as string) : null
    );
  }
}

/**
 * 安装上下文
 */
export const installEnv = async (metadata: Metadata, versions: Record<string, string>, gatewayPort: number) => {
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

  // fetch, XMLHttpRequest 函数将会被 http.std.dweb/fetch 重写, websocket 将会被 http.std.dweb/websocket 重写
  Object.defineProperties(globalThis, {
    fetch: {
      value: httpFetch,
    },
    XMLHttpRequest: {
      value: DwebXMLHttpRequest,
    },
    WebSocket: {
      value: class extends WebSocket {
        constructor(url: string | URL, protocols?: string | string[] | undefined) {
          let input = "wss://http.std.dweb/websocket";
          if (/iPhone|iPad|iPod/i.test(navigator.userAgent)) {
            input = `ws://localhost:${gatewayPort}?X-Dweb-Url=${input.replace("wss:", "ws:")}`;
          }

          if (typeof url === "string") {
            input += `?url=${url}`;
          } else if (url instanceof URL) {
            input += `?url=${url.href}`;
          }

          super(input, protocols);
        }
      },
    },
  });

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
