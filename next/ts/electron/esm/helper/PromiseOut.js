/**
 * @param value
 * @returns
 * @inline
 */
export const isPromiseLike = (value) => {
    return (value instanceof Object &&
        typeof value.then === "function");
};
/**
 * @param value
 * @returns
 * @inline
 */
export const isPromise = (value) => {
    return value instanceof Promise;
};
export class PromiseOut {
    static resolve(v) {
        const po = new PromiseOut();
        po.resolve(v);
        return po;
    }
    static sleep(ms) {
        const po = new PromiseOut();
        let ti = setTimeout(() => {
            ti = undefined;
            po.resolve();
        }, ms);
        po.onFinished(() => ti !== undefined && clearTimeout(ti));
        return po;
    }
    constructor() {
        Object.defineProperty(this, "promise", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "is_resolved", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "is_rejected", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "is_finished", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "value", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "reason", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "resolve", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "reject", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_innerFinally", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_innerFinallyArg", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_innerThen", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_innerCatch", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        this.promise = new Promise((resolve, reject) => {
            this.resolve = (value) => {
                try {
                    if (isPromiseLike(value)) {
                        value.then(this.resolve, this.reject);
                    }
                    else {
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
                }
                catch (err) {
                    this.reject(err);
                }
            };
            this.reject = (reason) => {
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
    onSuccess(innerThen) {
        if (this.is_resolved) {
            this.__callInnerThen(innerThen);
        }
        else {
            (this._innerThen || (this._innerThen = [])).push(innerThen);
        }
    }
    onError(innerCatch) {
        if (this.is_rejected) {
            this.__callInnerCatch(innerCatch);
        }
        else {
            (this._innerCatch || (this._innerCatch = [])).push(innerCatch);
        }
    }
    onFinished(innerFinally) {
        if (this.is_finished) {
            this.__callInnerFinally(innerFinally);
        }
        else {
            (this._innerFinally || (this._innerFinally = [])).push(innerFinally);
        }
    }
    _runFinally() {
        if (this._innerFinally) {
            for (const innerFinally of this._innerFinally) {
                this.__callInnerFinally(innerFinally);
            }
            this._innerFinally = undefined;
        }
    }
    __callInnerFinally(innerFinally) {
        queueMicrotask(async () => {
            try {
                await innerFinally(this._innerFinallyArg);
            }
            catch (err) {
                console.error("Unhandled promise rejection when running onFinished", innerFinally, err);
            }
        });
    }
    _runThen() {
        if (this._innerThen) {
            for (const innerThen of this._innerThen) {
                this.__callInnerThen(innerThen);
            }
            this._innerThen = undefined;
        }
    }
    _runCatch() {
        if (this._innerCatch) {
            for (const innerCatch of this._innerCatch) {
                this.__callInnerCatch(innerCatch);
            }
            this._innerCatch = undefined;
        }
    }
    __callInnerThen(innerThen) {
        queueMicrotask(async () => {
            try {
                await innerThen(this.value);
            }
            catch (err) {
                console.error("Unhandled promise rejection when running onSuccess", innerThen, err);
            }
        });
    }
    __callInnerCatch(innerCatch) {
        queueMicrotask(async () => {
            try {
                await innerCatch(this.value);
            }
            catch (err) {
                console.error("Unhandled promise rejection when running onError", innerCatch, err);
            }
        });
    }
}
