import { PromiseOut } from "./PromiseOut.ts";

class Locker extends PromiseOut<void> {
  constructor(readonly owner: unknown) {
    super();
  }
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
  private get _lastLocker(): undefined | PromiseOut<void> {
    return this._lockers[this._lockers.length - 1];
  }
  lock(owner?: unknown) {
    const pre_locker = this._lastLocker;
    const locker = new Locker(owner);
    this._lockers.push(locker);
    return pre_locker?.promise;
  }
  unlock(owner?: unknown) {
    const index = this._lockers.findIndex((locker) => locker.owner === owner);
    if (index !== -1) {
      const locker = this._lockers.splice(index, 1)[0];
      locker.resolve();
    }
  }
  async withLock(cb: () => unknown) {
    await this.lock();
    try {
      await cb();
    } finally {
      this.unlock();
    }
  }
}
