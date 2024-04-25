import { Signal, type $Callback, type $OffListener } from "./createSignal.ts";

export class StateSignal<T> implements $ReadyonlyStateSignal<T> {
  #state: T;
  get state() {
    return this.#state;
  }
  #signal = new Signal<$Callback<[T]>>();
  constructor(state: T, readonly equals: (newValue: T, oldValue: T) => boolean = (a, b) => a === b) {
    this.#state = state;
  }
  emit = (state: T) => {
    if (!this.equals(state, this.#state)) {
      this.#state = state;
      this.#signal.emit(state);
    }
  };
  emitAndClear = (state: T) => {
    if (!this.equals(state, this.#state)) {
      this.#state = state;
      this.#signal.emitAndClear(state);
    } else {
      this.#signal.clear();
    }
  };
  clear = () => {
    this.#signal.clear();
  };

  asReadyonly() {
    return this as $ReadyonlyStateSignal<T>;
  }

  listen = (cb: $Callback<[T]>) => {
    cb(this.#state);
    return this.#signal.listen(cb);
  };
}

export interface $ReadyonlyStateSignal<T> {
  state: T;
  listen: (cb: $Callback<[T]>) => $OffListener;
}
