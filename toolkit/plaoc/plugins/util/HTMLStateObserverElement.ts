import type { $OffListener } from "@dweb-browser/helper/createSignal.ts";
import { StateObserver } from "./StateObserver.ts";

export class HTMLStateObserverElement<RAW, STATE> extends HTMLElement {
  constructor(readonly state: StateObserver<RAW, STATE>) {
    super();
    state.startObserveState();
  }
  #onStateChange?: $OffListener;
  async connectedCallback() {
    this.#onStateChange = this.state.onChange((info) => {
      this.dispatchEvent(new CustomEvent("statechange", { detail: info }));
    });
    await this.state.getState(true); // 强制刷新
  }
  async disconnectedCallback() {
    if (this.#onStateChange) {
      this.#onStateChange();
      this.#onStateChange = undefined;
    }
    await this.state.stopObserve();
  }
}
export class HTMLIteratorObserverElement<STATE> extends HTMLElement {
  constructor(readonly state: Iterable<STATE>) {
    super();
    (async () => {
      for await (const info of state) {
        // console.log("stateChange", _info);
        this.dispatchEvent(new CustomEvent("statechange", { detail: info }));
      }
    })();
  }
}
