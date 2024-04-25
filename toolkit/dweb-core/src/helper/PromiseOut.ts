/**
 * @param value
 * @returns
 * @inline
 */
export const isPromiseLike = <T extends unknown = unknown>(value: T | unknown): value is PromiseLike<Awaited<T>> => {
  return value instanceof Object && typeof (value as PromiseLike<unknown>).then === "function";
};

/**
 * @param value
 * @returns
 * @inline
 */
export const isPromise = <T extends unknown = unknown>(value: T | unknown): value is Promise<Awaited<T>> => {
  return value instanceof Promise;
};
type $InnerFinallyArg<T> =
  | {
      readonly status: "resolved";
      readonly result: T;
    }
  | {
      readonly status: "rejected";
      readonly reason?: unknown;
    };
type $InnerFinally<T> = (arg: $InnerFinallyArg<T>) => unknown;
type $InnerThen<T> = (result: T) => unknown;
type $InnerCatch = (reason?: unknown) => unknown;
export class PromiseOut<T = unknown> {
  static resolve<T>(v: T) {
    const po = new PromiseOut<T>();
    po.resolve(v);
    return po;
  }
  static reject<T>(reason?: unknown) {
    const po = new PromiseOut<T>();
    po.reject(reason);
    return po;
  }
  static sleep(ms: number) {
    const po = new PromiseOut<void>();
    let ti: any = setTimeout(() => {
      ti = undefined;
      po.resolve();
    }, ms);
    po.onFinished(() => ti !== undefined && clearTimeout(ti));
    return po;
  }
  promise: Promise<T>;
  is_resolved = false;
  is_rejected = false;
  is_finished = false;
  value?: T;
  reason?: unknown;
  resolve!: (value: T | PromiseLike<T>) => void;
  reject!: (reason?: unknown) => void;
  private _innerFinally?: $InnerFinally<T>[];
  private _innerFinallyArg?: $InnerFinallyArg<T>;

  private _innerThen?: $InnerThen<T>[];
  private _innerCatch?: $InnerCatch[];

  constructor() {
    this.promise = new Promise<T>((resolve, reject) => {
      this.resolve = (value: T | PromiseLike<T>) => {
        try {
          if (isPromiseLike(value)) {
            value.then(this.resolve, this.reject);
          } else {
            this.is_resolved = true;
            this.is_finished = true;
            resolve((this.value = value));
            this._runThen();
            this._innerFinallyArg = Object.freeze({
              status: "resolved",
              result: this.value,
            });
            this._runFinally();
          }
        } catch (err) {
          this.reject(err);
        }
      };
      this.reject = (reason?: unknown) => {
        this.is_rejected = true;
        this.is_finished = true;
        reject((this.reason = reason));
        this._runCatch();
        this._innerFinallyArg = Object.freeze({
          status: "rejected",
          reason: this.reason,
        });
        this._runFinally();
      };
    });
  }
  onSuccess(innerThen: $InnerThen<T>) {
    if (this.is_resolved) {
      this.__callInnerThen(innerThen);
    } else {
      (this._innerThen || (this._innerThen = [])).push(innerThen);
    }
  }
  onError(innerCatch: $InnerCatch) {
    if (this.is_rejected) {
      this.__callInnerCatch(innerCatch);
    } else {
      (this._innerCatch || (this._innerCatch = [])).push(innerCatch);
    }
  }
  onFinished(innerFinally: () => unknown) {
    if (this.is_finished) {
      this.__callInnerFinally(innerFinally);
    } else {
      (this._innerFinally || (this._innerFinally = [])).push(innerFinally);
    }
  }
  private _runFinally() {
    if (this._innerFinally) {
      for (const innerFinally of this._innerFinally) {
        this.__callInnerFinally(innerFinally);
      }
      this._innerFinally = undefined;
    }
  }
  private __callInnerFinally(innerFinally: $InnerFinally<T>) {
    queueMicrotask(async () => {
      try {
        await innerFinally(this._innerFinallyArg!);
      } catch (err) {
        console.error("Unhandled promise rejection when running onFinished", innerFinally, err);
      }
    });
  }
  private _runThen() {
    if (this._innerThen) {
      for (const innerThen of this._innerThen) {
        this.__callInnerThen(innerThen);
      }
      this._innerThen = undefined;
    }
  }
  private _runCatch() {
    if (this._innerCatch) {
      for (const innerCatch of this._innerCatch) {
        this.__callInnerCatch(innerCatch);
      }
      this._innerCatch = undefined;
    }
  }
  private __callInnerThen(innerThen: $InnerThen<T>) {
    queueMicrotask(async () => {
      try {
        await innerThen(this.value!);
      } catch (err) {
        console.error("Unhandled promise rejection when running onSuccess", innerThen, err);
      }
    });
  }
  private __callInnerCatch(innerCatch: $InnerCatch) {
    queueMicrotask(async () => {
      try {
        await innerCatch(this.value!);
      } catch (err) {
        console.error("Unhandled promise rejection when running onError", innerCatch, err);
      }
    });
  }
}
