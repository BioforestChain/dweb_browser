import type { $Callback } from "./createSignal.ts";

export const promiseAsSignalListener = <T>(promise: Promise<T>) => {
  return (cb: $Callback<[T]>) => {
    promise.then(cb);
  };
};