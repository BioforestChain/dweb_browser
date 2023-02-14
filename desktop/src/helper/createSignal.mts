export const createSignal = <
  $Callback extends (...args: any[]) => unknown
>() => {
  return new Signal<$Callback>();
};

export class Signal<$Callback extends (...args: any[]) => unknown> {
  private _cbs = new Set<$Callback>();
  listen = (cb: $Callback) => {
    this._cbs.add(cb);
    return () => this._cbs.delete(cb);
  };

  emit = (...args: Parameters<$Callback>) => {
    for (const cb of this._cbs) {
      cb.apply(null, args);
    }
  };
}
