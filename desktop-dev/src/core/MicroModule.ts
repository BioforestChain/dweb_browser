import { once } from "../helper/$once.ts";
import { Mutex } from "../helper/Mutex.ts";
import { PromiseOut } from "../helper/PromiseOut.ts";
import { fetchExtends } from "../helper/fetchExtends/index.ts";
import { logger } from "../helper/logger.ts";
import { mapHelper } from "../helper/mapHelper.ts";
import { normalizeFetchArgs } from "../helper/normalizeFetchArgs.ts";
import { promiseAsSignalListener } from "../helper/promiseSignal.ts";
import { setHelper } from "../helper/setHelper.ts";
import type { $BootstrapContext } from "./bootstrapContext.ts";
import { Producer } from "./helper/Producer.ts";
import type { MICRO_MODULE_CATEGORY } from "./helper/category.const.ts";
import { $normalizeRequestInitAsIpcRequestArgs } from "./helper/ipcRequestHelper.ts";
import type { $IpcEvent, Ipc } from "./ipc/index.ts";
import {
  type $DWEB_DEEPLINK,
  type $IpcSupportProtocols,
  type $MMID,
  type $MicroModuleManifest,
  type $MicroModuleRuntime,
} from "./types.ts";

const enum MMState {
  BOOTSTRAP,
  SHUTDOWN,
}
export abstract class MicroModule {
  abstract manifest: $MicroModuleManifest;

  protected abstract createRuntime(context: $BootstrapContext): MicroModuleRuntime;

  @once()
  get console() {
    return logger(this);
  }
  toString() {
    return `MicroModule(${this.manifest.mmid})`;
  }

  get isRunning() {
    return this.#runtime?.isRunning === true;
  }

  #runtime?: MicroModuleRuntime;
  get runtime() {
    const runtime = this.#runtime;
    if (runtime === undefined) {
      throw new Error(`${this.manifest.mmid} is no running`);
    }
    return runtime;
  }
  async bootstrap(bootstrapContext: $BootstrapContext) {
    if (this.#runtime === undefined) {
      const runtime = this.createRuntime(bootstrapContext);
      runtime.onShutdown(() => {
        this.#runtime = undefined;
      });
      await runtime.bootstrap();
      this.#runtime = runtime;
    }
    return this.#runtime;
  }
}
export abstract class MicroModuleRuntime implements $MicroModuleRuntime {
  abstract mmid: $MMID;
  abstract ipc_support_protocols: $IpcSupportProtocols;
  abstract dweb_deeplinks: $DWEB_DEEPLINK[];
  abstract categories: MICRO_MODULE_CATEGORY[];
  abstract dir: $MicroModuleRuntime["dir"];
  abstract lang: $MicroModuleRuntime["lang"];
  abstract name: $MicroModuleRuntime["name"];
  abstract short_name: $MicroModuleRuntime["short_name"];
  abstract description: $MicroModuleRuntime["description"];
  abstract icons: $MicroModuleRuntime["icons"];
  abstract screenshots: $MicroModuleRuntime["screenshots"];
  abstract display: $MicroModuleRuntime["display"];
  abstract orientation: $MicroModuleRuntime["orientation"];
  abstract theme_color: $MicroModuleRuntime["theme_color"];
  abstract background_color: $MicroModuleRuntime["background_color"];
  abstract shortcuts: $MicroModuleRuntime["shortcuts"];
  abstract bootstrapContext: $BootstrapContext;
  abstract microModule: MicroModule;

  protected abstract _bootstrap(): unknown;
  protected abstract _shutdown(): unknown;

  private readonly stateLock = new Mutex();
  private state = MMState.SHUTDOWN;

  protected connectionLinks = new Set<Ipc>();
  protected connectionMap = new Map<$MMID, PromiseOut<Ipc>>();

  @once()
  get console() {
    return this.microModule.console;
  }

  /**
   * 内部程序与外部程序通讯的方法
   * TODO 这里应该是可以是多个
   */
  readonly #ipcConnectedProducer = new Producer<Ipc>("ipcConnect");
  /**
   * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
   * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
   * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
   */
  readonly onConnect = this.#ipcConnectedProducer.consumer("for-internal");

  get isRunning() {
    return this.state === MMState.BOOTSTRAP;
  }

  bootstrap() {
    return this.stateLock.withLock(async () => {
      if (this.state != MMState.BOOTSTRAP) {
        this.console.debug("bootstrap-start");
        await this._bootstrap();
        this.console.debug("bootstrap-end");
      } else {
        this.console.debug("bootstrap", `${this.mmid} already running`);
      }
      this.state = MMState.BOOTSTRAP;
    });
  }

  @once()
  private get beforeShotdownPo() {
    return new PromiseOut();
  }
  @once()
  get onBeforeShutdown() {
    return promiseAsSignalListener(this.beforeShotdownPo.promise);
  }
  @once()
  private get shutdownPo() {
    return new PromiseOut();
  }
  // get awaitShutdown(){return  this.shutdownPo.promise}
  @once()
  get onShutdown() {
    return promiseAsSignalListener(this.shutdownPo.promise);
  }

  shutdown() {
    return this.stateLock.withLock(async () => {
      this.beforeShotdownPo.resolve(undefined);
      await this._shutdown();
      this.shutdownPo.resolve(undefined);
      this.#ipcConnectedProducer.close();
    });
  }

  /**
   * 尝试连接到指定对象
   */
  connect(mmid: $MMID, auto_start = true) {
    return mapHelper.getOrPut(this.connectionMap, mmid, () => {
      const po = new PromiseOut<Ipc>();
      po.resolve(
        (async () => {
          const ipc = await this.bootstrapContext.dns.connect(mmid);
          if (auto_start) {
            void ipc.start();
          }
          return ipc;
        })()
      );
      return po;
    }).promise;
  }

  /**
   * 收到一个连接，触发相关事件
   */

  async beConnect(ipc: Ipc, reason?: Request) {
    if (setHelper.add(this.connectionLinks, ipc)) {
      this.console.debug("beConnect", ipc);
      ipc.onFork("beConnect").collect(async (forkEvent) => {
        ipc.console.debug("onFork", forkEvent.data);
        await this.beConnect(forkEvent.consume());
      });
      this.onBeforeShutdown(() => {
        return ipc.close();
      });
      ipc.onClosed(() => {
        this.connectionLinks.delete(ipc);
      });

      // 尝试保存到双向连接索引中
      if (this.connectionMap.has(ipc.remote.mmid) === false) {
        this.connectionMap.set(ipc.remote.mmid, PromiseOut.resolve(ipc));
      }
      this.#ipcConnectedProducer.send(ipc);
    }
  }
  getConnected(mmid: $MMID) {
    return this.connectionMap.get(mmid)?.promise;
  }

  // private async _nativeFetch(url: RequestInfo | URL, init?: RequestInit) {
  //   const { parsed_url, request_init } = normalizeFetchArgs(url, init);
  //   if (parsed_url.protocol === "file:") {
  //     const ipc = this.connect(parsed_url.hostname as $MMID);
  //     const reasonRequest = buildRequestX(parsed_url, request_init);
  //     return (await (await ipc).request(parsed_url.href, reasonRequest)).toResponse();
  //   }
  //   return fetch(parsed_url, request_init);
  // }

  // nativeFetch(url: RequestInfo | URL, init?: RequestInit) {
  //   if (init?.body instanceof ReadableStream) {
  //     Reflect.set(init, "duplex", "half");
  //   }
  //   return Object.assign(this._nativeFetch(url, init), fetchExtends);
  // }

  // protected abstract _nativeRequest(parsed_url: URL, request_init: RequestInit): Promise<IpcResponse>;

  // /** 同 ipc.request，只不过使用 fetch 接口的输入参数 */
  // nativeRequest(url: RequestInfo | URL, init?: RequestInit) {
  //   const args = normalizeFetchArgs(url, init);
  //   return this._nativeRequest(args.parsed_url, args.request_init);
  // }
  private async _nativeFetch(url: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const { parsed_url, request_init } = normalizeFetchArgs(url, init);
    const hostName = parsed_url.hostname;
    if (hostName.endsWith(".dweb") && parsed_url.protocol === "file:") {
      // const tmp = this._ipcConnectsMap.get(hostName as $MMID);
      // console.log("🧊 connect=> ", hostName, tmp?.is_finished, tmp);
      const ipc = await this.connect(hostName as $MMID);
      const ipc_req_init = await $normalizeRequestInitAsIpcRequestArgs(request_init);
      // console.log("🧊 connect request=> ", ipc.isActivity, ipc.channelId, parsed_url.href);
      let ipc_response = await ipc.request(parsed_url.href, ipc_req_init);
      // console.log("🧊 connect response => ", ipc_response.statusCode, ipc.isActivity, parsed_url.href);
      if (ipc_response.statusCode === 401) {
        /// 尝试进行授权请求
        try {
          const permissions = await ipc_response.body.text();
          if (await this.requestDwebPermissions(permissions)) {
            /// 如果授权完全成功，那么重新进行请求
            ipc_response = await ipc.request(parsed_url.href, ipc_req_init);
          }
        } catch (e) {
          console.error("fail to request permission:", e);
        }
      }
      return ipc_response.toResponse(parsed_url.href);
    }
    // const ipc_response = await this._nativeRequest(parsed_url, request_init);
    // return ipc_response.toResponse(parsed_url.href);
    return fetch(parsed_url, request_init);
  }
  /**
   * 模拟fetch的返回值
   * 这里的做fetch的时候需要先connect
   */
  nativeFetch(url: RequestInfo | URL, init?: RequestInit) {
    return Object.assign(this._nativeFetch(url, init), fetchExtends);
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

  @once()
  private get _manifest() {
    return {
      mmid: this.mmid,
      name: this.name,
      short_name: this.short_name,
      ipc_support_protocols: this.ipc_support_protocols,
      dweb_deeplinks: this.dweb_deeplinks,
      categories: this.categories,
      dir: this.dir,
      lang: this.lang,
      description: this.description,
      icons: this.icons,
      screenshots: this.screenshots,
      display: this.display,
      orientation: this.orientation,
      theme_color: this.theme_color,
      background_color: this.background_color,
      shortcuts: this.shortcuts,
    } satisfies $MicroModuleManifest;
  }
  toManifest() {
    return this._manifest;
  }
}
type $OnIpcConnect = (ipc: Ipc, reason: Request) => unknown;
type $OnActivity = (event: $IpcEvent, ipc: Ipc) => unknown;
type $OnRenderer = (event: $IpcEvent, ipc: Ipc) => unknown;
