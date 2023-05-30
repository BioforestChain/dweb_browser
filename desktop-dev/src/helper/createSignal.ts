import { cacheGetter } from "./cacheGetter.ts";

// deno-lint-ignore no-explicit-any
export const createSignal = <CB extends $Callback<any[]> = $Callback>(
  autoStart?: boolean
) => {
  return new Signal<CB>(autoStart);
};

// deno-lint-ignore no-explicit-any
export class Signal<CB extends $Callback<any[]> = $Callback> {
  constructor(autoStart = true) {
    if (autoStart) {
      this.start();
    }
  }
  private _cbs = new Set<CB>();

  private _started = false;
  @cacheGetter()
  private get _cachedEmits() {
    return [] as Array<Parameters<CB>>;
  }
  start = () => {
    if (this._started) {
      return;
    }
    this._started = true;
    if (this._cachedEmits.length) {
      for (const args of this._cachedEmits) {
        this._emit(args);
      }
      this._cachedEmits.length = 0;
    }
  };
  listen = (cb: CB): $OffListener => {
    this._cbs.add(cb);
    this.start();
    return () => this._cbs.delete(cb);
  };

  emit = (...args: Parameters<CB>) => {
    if (this._started) {
      this._emit(args);
    } else {
      this._cachedEmits.push(args);
    }
  };
  private _emit = (args: Parameters<CB>) => {
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
