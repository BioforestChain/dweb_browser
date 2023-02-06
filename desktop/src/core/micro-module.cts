import { fetchExtends } from "../helper/$makeFetchExtends.cjs";
import { PromiseOut } from "../helper/PromiseOut.cjs";
import type { $MicroModule, $MMID, $PromiseMaybe } from "../helper/types.cjs";
import type { Ipc } from "./ipc/index.cjs";

export abstract class MicroModule implements $MicroModule {
  abstract mmid: $MMID;
  running = false;
  protected before_bootstrap() {
    if (this.running) {
      throw new Error(`module ${this.mmid} alreay running`);
    }
    this.running = true;
  }
  private _bootstrap_lock?: Promise<void>;
  protected abstract _bootstrap(): unknown;
  protected after_bootstrap() {}
  async bootstrap() {
    this.before_bootstrap();

    const bootstrap_lock = new PromiseOut<void>();
    this._bootstrap_lock = bootstrap_lock.promise;
    try {
      await this._bootstrap();
    } finally {
      bootstrap_lock.resolve();
      this._bootstrap_lock = undefined;

      this.after_bootstrap();
    }
  }
  protected before_shutdown() {
    if (this.running === false) {
      throw new Error(`module ${this.mmid} alreay shutdown`);
    }
    this.running = false;
  }
  private _shutdown_lock?: Promise<void>;
  protected abstract _shutdown(): unknown;
  protected after_shutdown() {}
  async shutdown() {
    if (this._bootstrap_lock) {
      await this._bootstrap_lock;
    }

    const shutdown_lock = new PromiseOut<void>();
    this._shutdown_lock = shutdown_lock.promise;
    this.before_shutdown();
    try {
      await this._shutdown();
    } finally {
      shutdown_lock.resolve();
      this._shutdown_lock = undefined;

      this.after_shutdown();
    }
  }
  /** 外部程序与内部程序建立链接的方法 */
  protected abstract _connect(from: MicroModule): $PromiseMaybe<Ipc>;
  async connect(from: MicroModule) {
    if (this.running === false) {
      throw new Error("module no running");
    }
    await this._shutdown_lock;
    return this._connect(from);
  }

  fetch(url: RequestInfo | URL, init?: RequestInit) {
    /// 强制注入上下文
    return Object.assign(fetch.call(this, url, init), fetchExtends);
  }
}
