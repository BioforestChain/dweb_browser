import { fetchExtends } from "../helper/$makeFetchExtends.cjs";
import { createSignal } from "../helper/createSignal.cjs";
import { normalizeFetchArgs } from "../helper/normalizeFetchArgs.cjs";
import { PromiseOut } from "../helper/PromiseOut.cjs";
import type {
  $IpcSupportProtocols,
  $MicroModule,
  $MMID,
} from "../helper/types.cjs";
import { nativeFetchAdaptersManager } from "../sys/dns/nativeFetch.cjs";
import type { $BootstrapContext } from "./bootstrapContext.cjs";
import type { Ipc } from "./ipc/index.cjs";

export abstract class MicroModule implements $MicroModule {
  abstract ipc_support_protocols: $IpcSupportProtocols;
  abstract mmid: $MMID;
  get isRunning() {
    return this._running_state_lock.promise;
  }
  private _running_state_lock = PromiseOut.resolve(false);
  protected async before_bootstrap(context: $BootstrapContext) {
    if (await this._running_state_lock.promise) {
      throw new Error(`module ${this.mmid} alreay running`);
    }
    this._running_state_lock = new PromiseOut();
  }

  protected abstract _bootstrap(context: $BootstrapContext): unknown;

  protected async after_bootstrap(context: $BootstrapContext) {
    this._running_state_lock.resolve(true);
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
  }
  protected abstract _shutdown(): unknown;

  protected readonly _after_shutdown_signal = createSignal<() => unknown>();

  protected after_shutdown() {
    this._after_shutdown_signal.emit();
    this._after_shutdown_signal.clear();
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

  protected _ipcSet = new Set<Ipc>();
  /**
   * ??????????????????????????????????????????
   * TODO ??????????????????????????????
   */
  private readonly _connectSignal = createSignal<$OnIpcConnect>();

  /**
   * ?????????????????????????????? onConnect???????????????????????????????????????
   * ?????? NativeMicroModule ???????????????????????????????????????????????????????????? onConnect ??????
   * ????????? JsMicroModule ?????? onConnect ???????????? WebWorker ?????????
   */
  protected onConnect(cb: $OnIpcConnect) {
    return this._connectSignal.listen(cb);
  }
  async beConnect(ipc: Ipc, reason: Request) {
    this._ipcSet.add(ipc);
    ipc.onClose(() => {
      this._ipcSet.delete(ipc);
    });
    this._connectSignal.emit(ipc, reason);
  }

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
    return Object.assign(this._nativeFetch(url, init), fetchExtends);
  }
}

type $OnIpcConnect = (ipc: Ipc, reason: Request) => unknown;
