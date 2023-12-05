import { PromiseOut } from "../helper/PromiseOut.ts";
import { CacheGetter } from "../helper/cacheGetter.ts";
import { createSignal } from "../helper/createSignal.ts";
import { fetchExtends } from "../helper/fetchExtends/index.ts";
import { normalizeFetchArgs } from "../helper/normalizeFetchArgs.ts";
import { nativeFetchAdaptersManager } from "../sys/dns/nativeFetch.ts";
import type { $BootstrapContext } from "./bootstrapContext.ts";
import type { MICRO_MODULE_CATEGORY } from "./category.const.ts";
import type { Ipc, IpcEvent } from "./ipc/index.ts";
import type { $DWEB_DEEPLINK, $IpcSupportProtocols, $MMID, $MicroModule, $MicroModuleManifest } from "./types.ts";
import { MWEBVIEW_LIFECYCLE_EVENT } from "./types.ts";

export abstract class MicroModule implements $MicroModule {
  abstract mmid: $MMID;
  abstract ipc_support_protocols: $IpcSupportProtocols;
  abstract dweb_deeplinks: $DWEB_DEEPLINK[];
  abstract categories: MICRO_MODULE_CATEGORY[];
  abstract dir: $MicroModule["dir"];
  abstract lang: $MicroModule["lang"];
  abstract name: $MicroModule["name"];
  abstract short_name: $MicroModule["short_name"];
  abstract description: $MicroModule["description"];
  abstract icons: $MicroModule["icons"];
  abstract screenshots: $MicroModule["screenshots"];
  abstract display: $MicroModule["display"];
  abstract orientation: $MicroModule["orientation"];
  abstract theme_color: $MicroModule["theme_color"];
  abstract background_color: $MicroModule["background_color"];
  abstract shortcuts: $MicroModule["shortcuts"];

  protected abstract _bootstrap(context: $BootstrapContext): unknown;
  protected abstract _shutdown(): unknown;

  private _running_state_lock = PromiseOut.resolve(false);
  private _after_shutdown_signal = createSignal<() => unknown>();
  readonly onAfterShutdown = this._after_shutdown_signal.listen;
  protected _ipcSet = new Set<Ipc>();

  public addToIpcSet(ipc: Ipc) {
    if (this._running_state_lock.value === true) {
      void ipc.ready();
    }
    this._ipcSet.add(ipc);
    ipc.onClose(() => {
      this._ipcSet.delete(ipc);
    });
  }

  /**
   * 内部程序与外部程序通讯的方法
   * TODO 这里应该是可以是多个
   */
  private readonly _connectSignal = createSignal<$OnIpcConnect>();
  get isRunning() {
    return this._running_state_lock.promise;
  }

  protected context?: $BootstrapContext;

  protected async before_bootstrap(context: $BootstrapContext) {
    if (await this._running_state_lock.promise) {
      throw new Error(`module ${this.mmid} alreay running`);
    }
    this._running_state_lock = new PromiseOut();
    this.context = context;
  }

  protected after_bootstrap(_context: $BootstrapContext) {
    this._running_state_lock.resolve(true);
    /// 默认承认ready协议的存在，并在模块启动完成后，通知对方ready了
    this.onConnect((ipc) => {
      void ipc.ready();
    });
    for (const ipc of this._ipcSet) {
      void ipc.ready();
    }
  }

  async bootstrap(context: $BootstrapContext) {
    await this.before_bootstrap(context);
    try {
      await this._bootstrap(context);
    } finally {
      this.after_bootstrap(context);
    }
  }

  protected async before_shutdown() {
    if (false === (await this._running_state_lock.promise)) {
      throw new Error(`module ${this.mmid} already shutdown`);
    }
    this._running_state_lock = new PromiseOut();
    this.context = undefined;

    /// 关闭所有的通讯
    for (const ipc of this._ipcSet) {
      ipc.close();
    }
    this._ipcSet.clear();
  }

  protected after_shutdown() {
    this._after_shutdown_signal.emitAndClear();
    this._activitySignal.clear();
    this._connectSignal.clear();
    this._running_state_lock.resolve(false);
  }

  async shutdown() {
    await this.before_shutdown();
    try {
      await this._shutdown();
    } finally {
      this.after_shutdown();
    }
  }

  /**
   * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
   * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
   * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
   */
  protected onConnect(cb: $OnIpcConnect) {
    return this._connectSignal.listen(cb);
  }

  /**
   * 尝试连接到指定对象
   */
  async connect(mmid: $MMID) {
    if (this.context) {
      const [ipc] = await this.context.dns.connect(mmid);
      return ipc;
    }
  }

  /**
   * 收到一个连接，触发相关事件
   */
  beConnect(ipc: Ipc, reason: Request) {
    this.addToIpcSet(ipc);
    ipc.onEvent((event, ipc) => {
      if (event.name == MWEBVIEW_LIFECYCLE_EVENT.Activity) {
        this._activitySignal.emit(event, ipc);
      }
      if (event.name == MWEBVIEW_LIFECYCLE_EVENT.Renderer) {
        this._activitySignal.emit(event, ipc)
      }
    });
    this._connectSignal.emit(ipc, reason);
  }
  protected _activitySignal = createSignal<$OnActivity>();
  protected onActivity = this._activitySignal.listen;
  protected _rendererSignal = createSignal<$OnActivity>();
  protected onRenderer = this._activitySignal.listen;

  private async _nativeFetch(url: RequestInfo | URL, init?: RequestInit) {
    const args = normalizeFetchArgs(url, init);
    for (const adapter of nativeFetchAdaptersManager.adapters) {
      const response = await adapter(this, args.parsed_url, args.request_init);
      if (response !== undefined) {
        return response;
      }
    }
    return fetch(args.parsed_url, args.request_init);
  }

  nativeFetch(url: RequestInfo | URL, init?: RequestInit) {
    if (init?.body instanceof ReadableStream) {
      Reflect.set(init, "duplex", "half");
    }
    return Object.assign(this._nativeFetch(url, init), fetchExtends);
  }

  #manifest = new CacheGetter(() => {
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
  });
  toManifest() {
    return this.#manifest.value;
  }
}

type $OnIpcConnect = (ipc: Ipc, reason: Request) => unknown;
type $OnActivity = (event: IpcEvent, ipc: Ipc) => unknown;
