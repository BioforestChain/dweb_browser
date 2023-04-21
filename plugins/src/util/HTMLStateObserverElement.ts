import { $OffListener } from "../helper/createSignal.ts";
import { StateObserver } from "./StateObserver.ts";

export class HTMLStateObserverElement<RAW, STATE> extends HTMLElement {
  constructor(readonly state: StateObserver<RAW, STATE>) {
    super();
    (async () => {
      for await (const _info of state.jsonlines()) {
        // console.log("stateChange", info);
      }
    })();
  }
  #onStateChange?: $OffListener;
  async connectedCallback() {
    this.#onStateChange = this.state.onChange((info) => {
      this.dispatchEvent(new CustomEvent("statechange", { detail: info }));
    });
    await this.state.startObserve(); // 开始监听
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
