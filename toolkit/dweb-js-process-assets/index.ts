// deno-lint-ignore-file no-unused-vars
/// <reference lib="webworker"/>
/// 该文件是给 js-worker 用的，worker 中是纯粹的一个runtime，没有复杂的 import 功能，所以这里要极力克制使用外部包。
/// import 功能需要 chrome-80 才支持。我们明年再支持 import 吧，在此之前只能用 bundle 方案来解决问题
import type { $DWEB_DEEPLINK, $IpcSupportProtocols, $MicroModuleRuntime, $MMID } from "@dweb-browser/core/types.ts";

import { updateUrlOrigin } from "@dweb-browser/helper/fun/urlHelper.ts";
import { PromiseOut } from "@dweb-browser/helper/PromiseOut.ts";
export type { fetchExtends } from "@dweb-browser/helper/fetchExtends/index.ts";

import * as core from "./worker/std-dweb-core.ts";
import * as http from "./worker/std-dweb-http.ts";

import type { $BootstrapContext } from "@dweb-browser/core/bootstrapContext.ts";
import { onActivity } from "@dweb-browser/core/ipcEventOnActivity.ts";
import { onRenderer, onRendererDestroy } from "@dweb-browser/core/ipcEventOnRender.ts";
import { onShortcut } from "@dweb-browser/core/ipcEventOnShortcut.ts";
import { MicroModule, MicroModuleRuntime } from "@dweb-browser/core/MicroModule.ts";
import type { MICRO_MODULE_CATEGORY } from "@dweb-browser/core/type/category.const.ts";
import type { $PromiseMaybe } from "@dweb-browser/helper/$PromiseMaybe.ts";
import { once } from "@dweb-browser/helper/decorator/$once.ts";
import { mapHelper } from "@dweb-browser/helper/fun/mapHelper.ts";
import { addDebugTags } from "@dweb-browser/helper/logger.ts";
import type { $RunMainConfig } from "./main/index.ts";
import { createFetchHandler, Ipc, WebMessageEndpoint } from "./worker/std-dweb-core.ts";

declare global {
  interface DWebCore {
    jsProcess: JsProcessMicroModuleRuntime;
    core: typeof core;
    ipc: typeof core;
    http: typeof http;
    versions: { jsMicroModule: string };
    brands: { brand: string; version: string; fullVersion?: string };
    version: number;
    patch: number;
    gatewayPort: number;
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
  constructor(readonly meta: Metadata, private nativeFetchPort: MessagePort) {
    super();
    this.manifest = this.meta.data;
    this.ipcPool = new core.IpcPool(this.meta.data.mmid);
    this.fetchIpc = this.ipcPool.createIpc(
      new WebMessageEndpoint(this.nativeFetchPort, "fetch"),
      0,
      this.manifest,
      this.manifest,
      true
    );
    const debug = meta.envStringOrNull("debug");
    if (debug) {
      let tags: string[] | undefined;
      try {
        if (debug.startsWith("[")) {
          tags = JSON.parse(debug);
        }
        // deno-lint-ignore no-empty
      } catch {}
      if (tags === undefined) {
        tags = debug.split(/[,\s]/);
      }
      addDebugTags(tags);
    }
  }
  override manifest;
  readonly ipcPool;
  readonly fetchIpc;
  protected createRuntime(context: $BootstrapContext) {
    return new JsProcessMicroModuleRuntime(this, context);
  }
  private get bootstrapContext() {
    const waitMap = new Map<$MMID, PromiseOut<Ipc>>();
    this.fetchIpc.onEvent("wait-dns-connect").collect((event) => {
      event.consumeMapNotNull(async (ipcEvent) => {
        if (ipcEvent.name === "dns/connect/done") {
          this.console.verbose("connect-done", ipcEvent.data);
          const done = JSON.parse(core.IpcEvent.text(ipcEvent)) as {
            connect: $MMID;
            result: $MMID;
          };
          const po = waitMap.get(done.connect);
          if (po === undefined) {
            this.console.error(`no found connect task: ${done.connect}`);
            return;
          }
          const ipc = await this.runtime.getConnected(done.result);
          if (ipc === undefined) {
            po.reject(new Error(`no found connect result: ${done.result}`));
          } else {
            po.resolve(ipc);
          }
          return done;
        } else if (ipcEvent.name === "dns/connect/error") {
          this.console.error("connect-error", ipcEvent.data);
          const error = JSON.parse(core.IpcEvent.text(ipcEvent)) as {
            connect: $MMID;
            reason: string;
          };
          const po = mapHelper.getAndRemove(waitMap, error.connect);
          if (po === undefined) {
            this.console.error(`no found connect task: ${error.connect}`);
            return;
          }
          po.reject(error.reason);
        }
      });
    });

    const dnsRequest = async (pathname: string) => {
      const dnsIpc = await this.runtime.connect("dns.std.dweb");
      return await dnsIpc.request(`file://dns.std.dweb${pathname}`);
    };
    const ctx = {
      dns: {
        connect: (mmid: `${string}.dweb`, reason?: Request | undefined): $PromiseMaybe<core.Ipc> => {
          this.console.verbose("connect", mmid);
          const ipc = waitMap.get(mmid);
          if (ipc) {
            return ipc.promise;
          }
          const po = new PromiseOut<Ipc>();
          waitMap.set(mmid, po);
          po.onFinished(() => {
            waitMap.delete(mmid);
          });
          // 发送指令
          this.fetchIpc.postMessage(core.IpcEvent.fromText("dns/connect", mmid));

          return po.promise;
        },
        async install(mmid: `${string}.dweb`): Promise<void> {
          await dnsRequest(`/install?app_id=${mmid}`);
        },
        uninstall: async function (mmid: `${string}.dweb`): Promise<boolean> {
          const response = await dnsRequest(`/install?app_id=${mmid}`);
          return (await response.body.text()) === "true";
        },
        async query(mmid: `${string}.dweb`): Promise<core.$MicroModuleManifest | undefined> {
          const response = await dnsRequest(`/query?app_id=${mmid}`);
          const manifest = await response.body.text();
          if (manifest === "") {
            return undefined;
          }
          return JSON.parse(manifest);
        },
        async queryDeeplink(url: string): Promise<core.$MicroModuleManifest | undefined> {
          const response = await dnsRequest(`/queryDeeplink?deeplink=${url}`);
          const manifest = await response.body.text();
          if (manifest === "") {
            return undefined;
          }
          return JSON.parse(manifest);
        },
        async search(category: MICRO_MODULE_CATEGORY): Promise<core.$MicroModuleManifest[]> {
          const response = await dnsRequest(`/search?category=${category}`);
          const manifest = await response.body.text();
          if (manifest === "") {
            return [];
          }
          return JSON.parse(manifest);
        },
        async open(mmid: `${string}.dweb`): Promise<boolean> {
          const response = await dnsRequest(`/open?app_id=${mmid}`);
          return (await response.body.text()) === "true";
        },
        async close(mmid: `${string}.dweb`): Promise<boolean> {
          const response = await dnsRequest(`/mmid?app_id=${mmid}`);
          return (await response.body.text()) === "true";
        },
        restart: async (mmid: `${string}.dweb`) => {
          await dnsRequest(`/restart?app_id=${mmid}`);
        },
      },
    } satisfies $BootstrapContext;
    this.fetchIpc.onClosed(() => {
      console.debug("fetch-ipc closed, then close the js-process-worker.");
      workerGlobal.close();
    });
    return ctx;
  }
  override async bootstrap() {
    return (await super.bootstrap(this.bootstrapContext)) as unknown as JsProcessMicroModuleRuntime;
  }
}
export class JsProcessMicroModuleRuntime extends MicroModuleRuntime {
  override async connect(mmid: $MMID, auto_start?: boolean) {
    switch (mmid) {
      case "js.browser.dweb":
        return this.fetchIpc;
      case "file.std.dweb": {
        const fileIpc = await this.fileIpcPo;
        void fileIpc.start();
        return fileIpc;
      }
      case "permission.std.dweb": {
        const permissionIpc = await this.permissionIpcPo;
        void permissionIpc.start();
        return permissionIpc;
      }
    }
    return await super.connect(mmid, auto_start);
  }
  protected override _bootstrap() {
    // 自动转发来自 fetch-ipc 的请求
    this.fetchIpc.onRequest("proxy-request").collect(async (reqEvent) => {
      const request = reqEvent.consumeFilter((request) => {
        const req_url = request.parsed_url;
        return (
          (req_url.protocol === "file:" &&
            req_url.hostname.endsWith(".dweb") &&
            // 这里不消费自己的 request，只做代理
            req_url.hostname !== this.fetchIpc.locale.mmid) ||
          (req_url.protocol === "dweb:" &&
            // 这里不消费自己的 deeplink，只做代理
            this.dweb_deeplinks.some((dp) => request.url.startsWith(dp)) === false)
        );
      });
      if (request) {
        console.log("proxy-request", request);
        const response = await this.nativeFetch(request.toPureClientRequest());
        return request.ipc.postMessage(await core.IpcResponse.fromResponse(request.reqId, response, request.ipc));
      }
    });
    const _beConnect = async (event: MessageEvent) => {
      const data = event.data;
      if (Array.isArray(data) === false) {
        return;
      }
      const IPC_CONNECT_PREFIX = "ipc-connect/";
      if (typeof data[0] === "string" && data[0].startsWith(IPC_CONNECT_PREFIX)) {
        this.console.verbose("ipc-connect", data);
        const mmid = data[0].slice(IPC_CONNECT_PREFIX.length);
        const port = event.ports[0];
        const endpoint = new WebMessageEndpoint(port, mmid);
        const manifest: core.$MicroModuleManifest = data[1];
        const auto_start: boolean = data[2];

        const ipc = this.ipcPool.createIpc(
          endpoint,
          0,
          manifest,
          manifest,
          auto_start // 等一切准备完毕再手动启动
        );
        // 强制保存到连接池中
        mapHelper.getOrPut(this.connectionMap, ipc.remote.mmid, () => new PromiseOut()).resolve(ipc);
        // 触发beConnect
        await this.beConnect(ipc);
        workerGlobal.postMessage(`ipc-be-connect/${mmid}`);
      }
    };

    workerGlobal.addEventListener("message", _beConnect);
    this.onBeforeShutdown(() => {
      workerGlobal.removeEventListener("message", _beConnect);
    });

    this.onConnect("for-auto-start").collect((event) => {
      return event.consume().start(false, "do-auto-start");
    });
  }
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
  readonly ipcPool;
  readonly fetchIpc;
  readonly fileIpcPo;
  readonly permissionIpcPo;
  readonly meta;

  constructor(
    override readonly microModule: JsProcessMicroModule,
    override readonly bootstrapContext: $BootstrapContext
  ) {
    super();

    this.ipcPool = this.microModule.ipcPool;
    this.fetchIpc = this.microModule.fetchIpc;
    const waitProxyIpcTunnel = (key: string) => {
      const waiter = this.fetchIpc.onEvent(`wait-${key}-ipc-pid`);
      const pid_po = new PromiseOut<number>();
      waiter.collect((event) => {
        event.consumeFilter((ipcEvent) => {
          if (ipcEvent.name === `${key}-ipc-pid`) {
            pid_po.resolve(parseInt(core.IpcEvent.text(ipcEvent)));
            waiter.close();
            return true;
          }
          return false;
        });
      });
      return pid_po.promise.then((pid) => {
        return this.fetchIpc.waitForkedIpc(pid).then((fileIpc) => {
          void fileIpc.start();
          return fileIpc;
        });
      });
    };

    this.fileIpcPo = waitProxyIpcTunnel("file");
    this.permissionIpcPo = waitProxyIpcTunnel("permission");
    this.connectionLinks.add(this.fetchIpc);
    this.meta = this.microModule.meta;

    this.mmid = this.meta.data.mmid;
    this.name = `js process of ${this.mmid}`;
    this.host = this.meta.envString("host");
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

  protected override async _getIpcForFetch(url: URL) {
    // 支持 file:///
    if (url.protocol === "file:" && url.hostname === "") {
      return await this.fileIpcPo;
    }
    return await super._getIpcForFetch(url);
  }

  @once()
  get routes() {
    const routes = createFetchHandler([]);
    this.onConnect(`for-routes`).collect((ipcConnectEvent) => {
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
    workerGlobal.addEventListener("message", function onFetchIpcChannel(event): void {
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

    /// worker的生命信号
    const processLive = `js-process-live-${crypto.randomUUID()}`;
    console.info("process live", processLive);
    navigator.locks?.request(processLive, () => {
      postMessage(processLive);
      return new PromiseOut().promise;
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
export const installEnv = async (metadata: Metadata, gatewayPort: number) => {
  const fetchPort = await waitFetchPort();
  const jmm = new JsProcessMicroModule(metadata, fetchPort);
  const jsProcess = await jmm.bootstrap();

  const jsMicroModule = metadata.envString("jsMicroModule");
  const [version, patch] = jsMicroModule.split(".").map((v) => parseInt(v));
  const brands = JSON.parse(metadata.envString("brands") ?? "[]");

  const dweb = {
    jsProcess,
    core,
    ipc: core,
    http,
    versions: { jsMicroModule },
    brands,
    version,
    patch,
    gatewayPort,
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
            input = `ws://localhost:${dweb.gatewayPort}?X-Dweb-Url=${input.replace("wss:", "ws:")}`;
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
  workerGlobal.addEventListener("message", async function updateGatewayPort(event) {
    const data = event.data;
    if (Array.isArray(event.data) === false) {
      return;
    }
    if (data[0] === "updateGatewayPort") {
      dweb.gatewayPort = data[1];
    }
  });
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
