import type { $Callback } from "./createSignal";

export const promiseAsSignalListener = <T>(promise: Promise<T>) => {
  return (cb: $Callback<[T]>) => {
    promise.then(cb);
  };
};
