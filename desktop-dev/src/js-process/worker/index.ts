/// <reference lib="webworker"/>
/// 该文件是给 js-worker 用的，worker 中是纯粹的一个runtime，没有复杂的 import 功能，所以这里要极力克制使用外部包。
/// import 功能需要 chrome-80 才支持。我们明年再支持 import 吧，在此之前只能用 bundle 方案来解决问题
import type { $DWEB_DEEPLINK, $IpcSupportProtocols, $MicroModuleRuntime, $MMID } from "../../core/types.ts";

import { PromiseOut } from "../../helper/PromiseOut.ts";
import { updateUrlOrigin } from "../../helper/urlHelper.ts";
export type { fetchExtends } from "../../helper/fetchExtends/index.ts";

import * as core from "./std-dweb-core.ts";
import * as http from "./std-dweb-http.ts";

import { type $BootstrapContext } from "../../core/bootstrapContext.ts";
import type { $PromiseMaybe } from "../../core/helper/types.ts";
import type { MICRO_MODULE_CATEGORY } from "../../core/index.ts";
import { onActivity } from "../../core/ipcEventOnActivity.ts";
import { onRenderer, onRendererDestroy } from "../../core/ipcEventOnRender.ts";
import { onShortcut } from "../../core/ipcEventOnShortcut.ts";
import { MicroModule, MicroModuleRuntime } from "../../core/MicroModule.ts";
import { once } from "../../helper/$once.ts";
import type { $RunMainConfig } from "../main/index.ts";
import { createFetchHandler, Ipc, WebMessageEndpoint } from "./std-dweb-core.ts";

declare global {
  interface DWebCore {
    jsProcess: JsProcessMicroModuleRuntime;
    core: typeof core;
    ipc: typeof core;
    http: typeof http;
    versions: { jsMicroModule: string };
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

export class Metadata {
  constructor(readonly data: core.$MicroModuleManifest, readonly env: Record<string, string>) {}
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

/// 这个文件是给所有的 js-worker 用的，所以会重写全局的 fetch 函数，思路与 dns 模块一致
/// 如果是在原生的系统中，不需要重写fetch函数，因为底层那边可以直接捕捉 fetch
/// 虽然 nwjs 可以通过 chrome.webRequest 来捕捉请求，但没法自定义相应内容
/// 所以这里的方案还是对 fetch 进行重写
/// 拦截到的 ipc-message 通过 postMessage 转发到 html 层，再有 html 层

/**
 * 这个是虚假的 $MicroModule，这里只是一个影子，指代 native 那边的 micro_module
 */
export class JsProcessMicroModule extends MicroModule {
  override manifest = this.meta.data;
  readonly ipcPool = new core.IpcPool(this.meta.data.mmid);
  readonly fetchIpc = this.ipcPool.createIpc(
    new WebMessageEndpoint(this.nativeFetchPort, "fetch"),
    0,
    this.manifest,
    this.manifest,
    true
  );
  protected createRuntime(context: $BootstrapContext) {
    return new JsProcessMicroModuleRuntime(this, context);
  }
  private get bootstrapContext() {
    const ctx: $BootstrapContext = {
      dns: {
        install: function (mm: MicroModule): void {
          throw new Error("jmm dns.install not implemented.");
        },
        uninstall: function (mm: `${string}.dweb`): Promise<boolean> {
          throw new Error("jmm dns.uninstall not implemented.");
        },
        connect: (mmid: `${string}.dweb`, reason?: Request | undefined): $PromiseMaybe<core.Ipc> => {
          const po = new PromiseOut<Ipc>();
          this.fetchIpc.postMessage(core.IpcEvent.fromText("dns/connect", mmid));

          // fetchIpc.onEvent("wait-dns-connect").collect(async (event) => {
          //   const ipcEvent = event.consumeFilter((ipcEvent) => ipcEvent.name === `dns/connect/done/${connectMmid}`);
          //   if (ipcEvent !== undefined) {
          //     const { connect, result } = JSON.parse(core.IpcEvent.text(ipcEvent));
          //     const task = this._ipcConnectsMap.get(connect);
          //     console.log("xxlife 收到桥接完成消息=>", task, ipcEvent.name, ipcEvent.data);
          //     if (task) {
          //       /// 这里之所以 connect 和 result 存在不一致的情况，是因为 subprotocol 的存在
          //       if (task.is_resolved === false) {
          //         const resultTask = this._ipcConnectsMap.get(result);
          //         if (resultTask && resultTask !== task) {
          //           task.resolve(await resultTask.promise);
          //         }
          //       }
          //       const ipc = await task.promise;
          //       // 手动启动,这里才是真正的建立完成通信
          //       ipc.start();
          //       await ipc.ready();
          //       console.log("xxlife 桥接建立完成=>", ipc.channelId, ipc.isActivity);
          //     }
          //   } else if (ipcEvent.name.startsWith("forward/")) {
          //     // 这里负责代理native端的请求
          //     const [_, action, mmid] = ipcEvent.name.split("/");
          //     const ipc = await this.connect(mmid as $MMID);
          //     if (action === "lifeCycle") {
          //       ipc.postMessage($normalizeIpcMessage(JSON.parse(ipcEvent.text), ipc));
          //     } else if (action === "request") {
          //       const response = await ipc.request(
          //         $normalizeIpcMessage(JSON.parse(ipcEvent.text), ipc) as IpcClientRequest
          //       );
          //       this.fetchIpc.postMessage(
          //         IpcEvent.fromText(`forward/response/${mmid}`, JSON.stringify(response.ipcResMessage()))
          //       );
          //     } else if (action === "close") {
          //       console.log("worker ipc close=>", ipc.channelId);
          //       ipc.close();
          //     }
          //   }
          // });

          const _beConnect = (event: MessageEvent) => {
            const data = event.data;
            if (Array.isArray(data) === false) {
              return;
            }
            if (data[0] === `ipc-connect/${mmid}`) {
              const port = event.ports[0];
              const endpoint = new WebMessageEndpoint(port, mmid);
              const manifest: core.$MicroModuleManifest = data[1];
              const env: Record<string, string> = data[2];
              Object.defineProperty(manifest, "env", { value: Object.freeze(env) });

              const ipc = this.ipcPool.createIpc(
                endpoint,
                0,
                manifest,
                manifest,
                false // 等一切准备完毕再手动启动
              );
              po.resolve(ipc);
              workerGlobal.removeEventListener("message", _beConnect);
            }
          };

          workerGlobal.addEventListener("message", _beConnect);
          return po.promise;
        },
        query: (mmid: `${string}.dweb`): Promise<core.$MicroModuleManifest | undefined> => {
          throw new Error("dns.query not implemented.");
        },
        search: (category: MICRO_MODULE_CATEGORY): Promise<core.$MicroModuleManifest[]> => {
          throw new Error("dns.search not implemented.");
        },
        open: (mmid: `${string}.dweb`): Promise<boolean> => {
          throw new Error("dns.open not implemented.");
        },
        close: (mmid: `${string}.dweb`): Promise<boolean> => {
          throw new Error("dns.close not implemented.");
        },
        restart: (mmid: `${string}.dweb`): void => {
          throw new Error("dns.restart not implemented.");
        },
      },
    };
    this.fetchIpc.onClosed(() => {
      console.debug("fetch-ipc closed, then close the js-process-worker.");
      workerGlobal.close();
    });
    return ctx;
  }
  constructor(readonly meta: Metadata, private nativeFetchPort: MessagePort) {
    super();
  }
  override async bootstrap() {
    return (await super.bootstrap(this.bootstrapContext)) as unknown as JsProcessMicroModuleRuntime;
  }
}
export class JsProcessMicroModuleRuntime extends MicroModuleRuntime {
  protected override _bootstrap() {}
  protected override async _shutdown() {
    await this.fetchIpc.close();
  }
  readonly mmid: $MMID;
  readonly name: string;
  readonly host: string;
  readonly dweb_deeplinks: $DWEB_DEEPLINK[] = [];
  readonly categories: $MicroModuleRuntime["categories"] = [];
  override dir: core.TextDirectionType | undefined;
  override lang: string | undefined;
  override short_name: string | undefined;
  override description: string | undefined;
  override icons: core.ImageResource[] | undefined;
  override screenshots: core.ImageResource[] | undefined;
  override display: core.DisplayModeType | undefined;
  override orientation:
    | "any"
    | "landscape"
    | "landscape-primary"
    | "landscape-secondary"
    | "natural"
    | "portrait"
    | "portrait-primary"
    | "portrait-secondary"
    | undefined;
  override theme_color: string | undefined;
  override background_color: string | undefined;
  override shortcuts: core.ShortcutItem[] | undefined;

  @once()
  get ipc_support_protocols() {
    return {
      json: true,
      cbor: true,
      protobuf: false,
    } satisfies $IpcSupportProtocols;
  }
  readonly ipcPool = this.microModule.ipcPool;
  readonly fetchIpc = this.microModule.fetchIpc;
  readonly meta = this.microModule.meta;

  constructor(
    override readonly microModule: JsProcessMicroModule,
    override readonly bootstrapContext: $BootstrapContext
  ) {
    super();

    this.mmid = this.meta.data.mmid;
    this.name = `js process of ${this.mmid}`;
    this.host = this.meta.envString("host");

    // // 整个worker关闭
    // this.fetchIpc.onClosed(async () => {
    //   console.log("worker-close=>", this.fetchIpc.channelId, this.mmid);
    //   // 当worker关闭的时候，触发关闭，让用户可以基于这个事件释放资源
    //   this._onCloseSignal.emit(IpcEvent.fromText("close", this.mmid), this.fetchIpc);
    //   // 销毁所有ipc
    //   await Promise.all(
    //     Array.from(this._ipcConnectsMap.values(), async (ipcPo) => {
    //       const ipc = await ipcPo.promise;
    //       ipc.close();
    //     })
    //   );
    //   workerGlobal.close();
    // });
    // this.fetchIpc.onEvent(async (ipcEvent) => {
    //   if (ipcEvent.name === "dns/connect/done" && typeof ipcEvent.data === "string") {
    //     const { connect, result } = JSON.parse(ipcEvent.data);
    //     const task = this._ipcConnectsMap.get(connect);
    //     console.log("xxlife 收到桥接完成消息=>", task, ipcEvent.name, ipcEvent.data);
    //     if (task) {
    //       /// 这里之所以 connect 和 result 存在不一致的情况，是因为 subprotocol 的存在
    //       if (task.is_resolved === false) {
    //         const resultTask = this._ipcConnectsMap.get(result);
    //         if (resultTask && resultTask !== task) {
    //           task.resolve(await resultTask.promise);
    //         }
    //       }
    //       const ipc = await task.promise;
    //       // 手动启动,这里才是真正的建立完成通信
    //       ipc.start();
    //       await ipc.ready();
    //       console.log("xxlife 桥接建立完成=>", ipc.channelId, ipc.isActivity);
    //     }
    //   } else if (ipcEvent.name.startsWith("forward/")) {
    //     // 这里负责代理native端的请求
    //     const [_, action, mmid] = ipcEvent.name.split("/");
    //     const ipc = await this.connect(mmid as $MMID);
    //     if (action === "lifeCycle") {
    //       ipc.postMessage($normalizeIpcMessage(JSON.parse(ipcEvent.text), ipc));
    //     } else if (action === "request") {
    //       const response = await ipc.request($normalizeIpcMessage(JSON.parse(ipcEvent.text), ipc) as IpcClientRequest);
    //       this.fetchIpc.postMessage(
    //         IpcEvent.fromText(`forward/response/${mmid}`, JSON.stringify(response.ipcResMessage()))
    //       );
    //     } else if (action === "close") {
    //       console.log("worker ipc close=>", ipc.channelId);
    //       ipc.close();
    //     }
    //   }
    // });
  }

  get onActivity() {
    return onActivity.bind(null, this);
  }
  get onRenderer() {
    return onRenderer.bind(null, this);
  }
  get onRendererDestroy() {
    return onRendererDestroy.bind(null, this);
  }
  get onShortcut() {
    return onShortcut.bind(null, this);
  }

  // protected override async _nativeRequest(parsed_url: URL, request_init: RequestInit) {
  //   const ipc_req_init = await $normalizeRequestInitAsIpcRequestArgs(request_init);
  //   return await this.fetchIpc.request(parsed_url.href, ipc_req_init);
  // }

  @once()
  get routes() {
    const routes = createFetchHandler([]);
    this.onConnect.collect((ipcConnectEvent) => {
      ipcConnectEvent.data.onRequest("onFetch").collect((ipcRequestEvent) => {
        const ipcRequest = ipcRequestEvent.consume();
        routes(ipcRequest);
      });
    });
    return routes;
  }

  // 提供一个关闭通信的功能
  async close(cause?: string) {
    await this.fetchIpc.close(cause);
    this.ipcPool.destroy();
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
    postMessage("waiting-fetch-ipc");
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
export const installEnv = async (metadata: Metadata, gatewayPort: number) => {
  const fetchPort = await waitFetchPort();
  const jmm = new JsProcessMicroModule(metadata, fetchPort);
  const jsProcess = await jmm.bootstrap();

  const jsMicroModule = metadata.envString("jsMicroModule");
  const [version, patch] = jsMicroModule.split(".").map((v) => parseInt(v));

  const dweb = {
    jsProcess,
    core,
    ipc: core,
    http,
    versions: { jsMicroModule },
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

  // //#region
  // {
  //   const { http, jsProcess } = navigator.dweb;
  //   const httpServer = await http.createHttpDwebServer(jsProcess, { subdomain: "www" });
  //   jsProcess.fetchIpc.postMessage(
  //     core.IpcEvent.fromText("http-server", httpServer.startResult.urlInfo.buildDwebUrl().href)
  //   );
  //   await httpServer.listen((event) => {
  //     console.log("got request", event.ipcRequest.url);
  //     return { body: event.ipcRequest.url };
  //   });
  // }
  // //#endregion

  return jsProcess;
};
