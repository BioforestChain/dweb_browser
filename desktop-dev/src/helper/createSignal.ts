// deno-lint-ignore no-explicit-any
export const createSignal = <CB extends $Callback<any[]> = $Callback>(autoStart?: boolean) => {
  return new Signal<CB>(autoStart);
};

// deno-lint-ignore no-explicit-any
export class Signal<CB extends $Callback<any[]> = $Callback> {
  constructor(autoStart = true) {
    if (autoStart) {
      this.#start();
    }
  }
  private _cbs = new Set<CB>();

  #started = false;
  #cachedEmits: Array<Parameters<CB>> = [];
  #start = () => {
    if (this.#started) {
      return;
    }
    this.#started = true;
    if (this.#cachedEmits.length) {
      for (const args of this.#cachedEmits) {
        this.#emit(args, this._cbs);
      }
      this.#cachedEmits.length = 0;
    }
  };
  listen = (cb: CB): $OffListener => {
    this._cbs.add(cb);
    this.#start();
    return () => this._cbs.delete(cb);
  };

  emit = (...args: Parameters<CB>) => {
    if (this.#started) {
      this.#emit(args, this._cbs);
    } else {
      this.#cachedEmits.push(args);
    }
  };
  emitAndClear = (...args: Parameters<CB>) => {
    if (this.#started) {
      const cbs = [...this._cbs];
      this._cbs.clear();
      this.#emit(args, cbs);
    } else {
      this.#cachedEmits.push(args);
    }
  };
  #emit = (args: Parameters<CB>, cbs: Iterable<CB>) => {
    for (const cb of cbs) {
      try {
        cb.apply(null, args);
      } catch (reason) {
        // ignore
        console.warn(reason);
      }
    }
  };

  mapNotNull<T, R>(transform: (value: T) => R | undefined) {
    const signal = new Signal<CB>();
    return {
      collect(value) {
        // signal.listen(transform)
      }
    }
  }

  clear = () => {
    this._cbs.clear();
  };
}
export type $Callback<ARGS extends unknown[] = [], RETURN = unknown> = (...args: ARGS) => RETURN;

export type $OffListener = () => boolean;
