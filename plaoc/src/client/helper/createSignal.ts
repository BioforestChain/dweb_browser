// deno-lint-ignore no-explicit-any
export const createSignal = <CB extends $Callback<any[]> = $Callback>() => {
  const signal = new Signal<CB>();

  return signal;
};

// deno-lint-ignore no-explicit-any
export class Signal<CB extends $Callback<any[]> = $Callback> {
  private _cbs = new Set<CB>();
  listen = (cb: CB): $OffListener => {
    this._cbs.add(cb);
    return () => this._cbs.delete(cb);
  };

  emit = (...args: Parameters<CB>) => {
    for (const cb of this._cbs) {
      cb.apply(null, args);
    }
  };
  clear = () => {
    this._cbs.clear();
  };
}

export type $Callback<ARGS extends unknown[] = [], RETURN = unknown> = (
  ...args: ARGS
) => RETURN;

export type $OffListener = () => boolean;
