import { PromiseOut } from "./PromiseOut.ts";

export const debounce = <F extends (...args: any[]) => Promise<any> | void>(fun: F, ms = 0) => {
  let ti: any;
  let lock:
    | undefined
    | {
        ti: any;
        po: PromiseOut<any>;
        args: any[];
      };
  return ((...args: any[]) => {
    if (lock) {
      clearTimeout(lock.ti);
    } else {
      lock = {
        ti: undefined,
        po: new PromiseOut(),
        args: [],
      };
    }
    const l = lock;
    l.args = args;
    l.ti = setTimeout(() => {
      try {
        lock = undefined;
        l.po.resolve(fun(...l.args));
      } catch (err) {
        l.po.reject(err);
      }
    }, ms);
    return l.po.promise;
  }) as unknown as F;
};

const debounceNames = new Map<PropertyKey, () => any>();
export const debounceQueueMicrotask = <T extends () => any>(name: string, fun: T) => {
  if (debounceNames.has(name) === false) {
    queueMicrotask(() => {
      const fun = debounceNames.get(name)!;
      debounceNames.delete(name);
      fun();
    });
  }
  debounceNames.set(name, fun);
};
