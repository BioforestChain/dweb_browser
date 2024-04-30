// deno-lint-ignore no-explicit-any
export const createSignal = <CB extends $Callback<any[]> = $Callback>(autoStart?: boolean) => {
  return new Signal<CB>(autoStart);
};

// deno-lint-ignore no-explicit-any
export class Signal<CB extends $Callback<any[]> = $Callback> {
  constructor(autoStart = true) {
    if (autoStart) {
      this._start();
    }
  }
  private _cbs = new Set<CB>();

  #started = false;
  private _cachedActions: Array<() => void> = [];
  private _start() {
    if (this.#started) {
      return;
    }
    this.#started = true;
    if (this._cachedActions.length) {
      for (const action of this._cachedActions) {
        action();
      }
      this._cachedActions.length = 0;
    }
  }
  private _startAction(action: () => void) {
    if (this.#started) {
      action();
    } else {
      this._cachedActions.push(action);
    }
  }
  listen = (cb: CB): $OffListener => {
    this._cbs.add(cb);
    this._start();
    return () => this._cbs.delete(cb);
  };

  emit = (...args: Parameters<CB>) => {
    this._startAction(() => {
      this._emit(args, this._cbs);
    });
  };
  emitAndClear = (...args: Parameters<CB>) => {
    this._startAction(() => {
      const cbs = [...this._cbs];
      this._cbs.clear();
      this._emit(args, cbs);
    });
  };
  private _emit(args: Parameters<CB>, cbs: Iterable<CB>) {
    for (const cb of cbs) {
      try {
        cb.apply(null, args);
      } catch (reason) {
        // ignore
        console.warn(reason);
      }
    }
  }

  clear = () => {
    this._startAction(() => {
      this._cbs.clear();
    });
  };
}
export type $Callback<ARGS extends unknown[] = [], RETURN = unknown> = (...args: ARGS) => RETURN;

export type $OffListener = () => boolean;
