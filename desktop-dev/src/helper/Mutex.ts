import { PromiseOut } from "./PromiseOut.ts";

class Locker {
  readonly po = new PromiseOut<void>();
  constructor(pre_locker: Locker | undefined) {
    this.prev = pre_locker?.curr;
    this.curr = this.prev?.then(() => this.po.promise) ?? this.po.promise;
  }
  readonly prev: Promise<void> | undefined;
  readonly curr: Promise<void>;
}
export class Mutex {
  constructor(lock?: boolean) {
    if (lock) {
      void this.lock();
    }
  }
  get isLocked() {
    return this._lockers.length > 0;
  }
  private _lockers: Locker[] = [];
  private get _lastLocker() {
    return this._lockers[this._lockers.length - 1];
  }
  lock() {
    const locker = new Locker(this._lastLocker);
    this._lockers.push(locker);
    return locker.prev;
  }
  unlock() {
    const locker = this._lockers.shift();
    locker?.po.resolve();
  }
  async withLock<R>(cb: () => R) {
    await this.lock();
    try {
      return await cb();
    } finally {
      this.unlock();
    }
  }
}
