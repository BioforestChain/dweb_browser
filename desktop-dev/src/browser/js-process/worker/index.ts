/// <reference lib="webworker"/>
/// 该文件是给 js-worker 用的，worker 中是纯粹的一个runtime，没有复杂的 import 功能，所以这里要极力克制使用外部包。
/// import 功能需要 chrome-80 才支持。我们明年再支持 import 吧，在此之前只能用 bundle 方案来解决问题
import type { $DWEB_DEEPLINK, $IpcSupportProtocols, $MicroModule, $MMID } from "../../../core/types.ts";

import { $normalizeRequestInitAsIpcRequestArgs } from "../../../core/helper/ipcRequestHelper.ts";
import { $Callback, createSignal } from "../../../helper/createSignal.ts";
import { fetchExtends } from "../../../helper/fetchExtends/index.ts";
import { mapHelper } from "../../../helper/mapHelper.ts";
import { normalizeFetchArgs } from "../../../helper/normalizeFetchArgs.ts";
import { PromiseOut } from "../../../helper/PromiseOut.ts";
import { updateUrlOrigin } from "../../../helper/urlHelper.ts";
export type { fetchExtends } from "../../../helper/fetchExtends/index.ts";

import * as core from "./std-dweb-core.ts";
import * as http from "./std-dweb-http.ts";

import { $RunMainConfig } from "../main/index.ts";
import {
  $objectToIpcMessage,
  $OnFetch,
  $OnIpcEventMessage,
  $OnIpcRequestMessage,
  createFetchHandler,
  Ipc,
  IPC_HANDLE_EVENT,
  IpcError,
  IpcEvent,
  IpcRequest,
  MessagePortIpc,
} from "./std-dweb-core.ts";

declare global {
  type $JsProcessMicroModuleContructor = JsProcessMicroModule;
  const JsProcessMicroModule: new (mmid: $MMID) => $JsProcessMicroModuleContructor;

  interface DWebCore {
    jsProcess: $JsProcessMicroModuleContructor;
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
  readonly ipcPool: core.IpcPool;

  constructor(readonly meta: Metadata, private nativeFetchPort: MessagePort) {
    this.ipcPool = new core.IpcPool(meta.data.mmid);
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
        const port_po = mapHelper.getOrPut(this._ipcConnectsMap, mmid, () => {
          const ipc_po = new PromiseOut<MessagePortIpc>();
          ipc_po.onSuccess((ipc) => {
            ipc.onClose(() => {
              this._ipcConnectsMap.delete(mmid);
            });
          });
          return ipc_po;
        });
        // 这里创建的是netive的代理ipc（Native2JsIpc） (tip: 这里的通信并不是马上建立的，因为对方只是发送过来一个port1,native端端port2有可能还在一个map里)
        const ipc = this.ipcPool.create<MessagePortIpc>(`worker-createIpc-${mmid}`, {
          remote: {
            mmid,
            ipc_support_protocols,
            dweb_deeplinks: [],
            categories: [],
            name: this.name,
          },
          port: port,
          autoStart: false, // 等建立完成再手动启动
        });
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
          console.log("js-process onError=>", ipc.channelId, error.message, error.errorCode);
          this._ipcConnectsMap.get(mmid)?.reject(error);
        });
      }
    };
    workerGlobal.addEventListener("message", _beConnect);

    this.mmid = meta.data.mmid;
    this.name = `js process of ${this.mmid}`;
    this.host = this.meta.envString("host");
    // 这里真正的跟native端的create-process 建立通信
    this.fetchIpc = this.ipcPool.create(`create-process(fetchIpc)-${this.mmid}`, {
      remote: this,
      port: this.nativeFetchPort,
    });
    // 整个worker关闭
    this.fetchIpc.onClose(() => {
      console.log("worker-close=>", this.fetchIpc.channelId, this.mmid);
      this.ipcPool.close();
    });
    this.fetchIpc.onEvent(async (ipcEvent) => {
      if (ipcEvent.name === "dns/connect/done" && typeof ipcEvent.data === "string") {
        const { connect, result } = JSON.parse(ipcEvent.data);
        const task = this._ipcConnectsMap.get(connect);
        // console.log("dns/connect/done===>", connect, task, task?.is_resolved);
        if (task) {
          /// 这里之所以 connect 和 result 存在不一致的情况，是因为 subprotocol 的存在
          if (task.is_resolved === false) {
            const resultTask = this._ipcConnectsMap.get(result);
            if (resultTask && resultTask !== task) {
              task.resolve(await resultTask.promise);
            }
          }
          const ipc = await task.promise;
          // console.log("桥接建立完成=>", connect, ipc.channelId, result);
          // 手动启动
          ipc.start();
          // console.log("桥接建立完成=>", ipc.channelId, ipc.isActivity);
        }
      } else if (ipcEvent.name.startsWith("forward/")) {
        // 这里负责代理native端的请求
        const [_, action, mmid] = ipcEvent.name.split("/");
        const ipc = await this.connect(mmid as $MMID);
        if (action === "lifeCycle") {
          ipc.postMessage($objectToIpcMessage(JSON.parse(ipcEvent.text), ipc));
        } else if (action === "request") {
          const response = await ipc.request($objectToIpcMessage(JSON.parse(ipcEvent.text), ipc) as IpcRequest);
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
    // const tmp = this._ipcConnectsMap.get(hostName as $MMID);
    // console.log("🧊 connect=> ", hostName, tmp?.is_finished, tmp);
    const ipc = await this.connect(hostName as $MMID);
    const ipc_req_init = await $normalizeRequestInitAsIpcRequestArgs(args.request_init);
    // console.log("🧊 connect request=> ", ipc.isActivity, ipc.channelId, args.parsed_url.href);
    let ipc_response = await ipc.request(args.parsed_url.href, ipc_req_init);
    // console.log("🧊 connect response => ", ipc_response.statusCode, ipc.isActivity, args.parsed_url.href);
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
  // 存储worker connect的Ipc,也即在netive端createIpc方法中创建,并桥接的Ipc
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
    // console.log("ready==>", mmid, ipc.channelId, ipc.isActivity, mmid, ipc.remote.mmid);
    await this.afterIpcReady(ipc);
    // console.log("ready afterIpcReady===>", mmid, ipc.remote.mmid);
    return ipc;
  }

  private _ipcSet = new Set<Ipc>();
  async addToIpcSet(ipc: Ipc) {
    this._ipcSet.add(ipc);
    ipc.onClose(() => {
      this._ipcSet.delete(ipc);
    });
    await this.afterIpcReady(ipc);
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
  // deno-lint-ignore no-explicit-any
  close(reson?: any) {
    this.ipcPool.close();
    this._ipcConnectsMap.forEach(async (ipc) => {
      ipc.promise.then((res) => {
        res.postMessage(new IpcError(500, `worker error=>${reson}`));
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
