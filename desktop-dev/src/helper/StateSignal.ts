import { $Callback, $OffListener, Signal } from "./createSignal.ts";

export class StateSignal<T> extends Signal<$Callback<[T]>> implements $ReadyonlyStateSignal<T> {
  #state: T;
  constructor(state: T) {
    super();
    this.#state = state;
    this.listen((state) => {
      this.#state = state;
    });
  }

  get state() {
    return this.#state;
  }

  asReadyonly() {
    return this as $ReadyonlyStateSignal<T>;
  }
}

export interface $ReadyonlyStateSignal<T> {
  state: T;
  listen: (cb: $Callback<[T]>) => $OffListener;
}
