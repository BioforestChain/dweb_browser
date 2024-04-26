import { PromiseOut } from "@dweb-browser/helper/PromiseOut.ts";
import { SafeEvent, SafeEventTarget, SafeStateEvent } from "../../helper/SafeEventTarget.ts";
import type { $Callback, Signal } from "../../helper/createSignal.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import type { $Modal, $ModalCallback } from "./window.type.ts";

export const enum WindowModalState {
  INIT = "init",
  OPENING = "opening",
  OPEN = "open",
  CLOSING = "closing",
  CLOSE = "close",
  DESTROYING = "destroying",
  DESTROY = "destroy",
}
type $ModalEvents = {
  close: SafeEvent<WindowModalState.CLOSE>;
  open: SafeEvent<WindowModalState.OPEN>;
  destroy: SafeEvent<WindowModalState.DESTROY>;
  statechange: SafeStateEvent<"statechange", WindowModalState>;
};

export class WindowModalController extends SafeEventTarget<$ModalEvents> {
  constructor(readonly plugin: BasePlugin, readonly modal: $Modal, onCallback: Signal<$Callback<[$ModalCallback]>>) {
    super();
    onCallback.listen((callbackData) => {
      switch (callbackData.type) {
        case "open":
          this.setState(WindowModalState.OPEN);
          break;
        case "close":
          this.setState(WindowModalState.CLOSE);
          break;
        case "close-alert":
          this.setState(WindowModalState.CLOSE);
          break;
      }
    });
    this._init();
  }
  protected _init() {}
  #state = WindowModalState.INIT;
  private setState(value: WindowModalState) {
    if (value === this.#state) {
      return;
    }
    this.#state = value;
    this.dispatchEvent(new SafeStateEvent("statechange", { state: value }));
    switch (value) {
      case WindowModalState.OPEN:
        this.dispatchEvent(new SafeEvent(WindowModalState.OPEN));
        break;
      case WindowModalState.CLOSE:
        this.dispatchEvent(new SafeEvent(WindowModalState.CLOSE));
        break;
      case WindowModalState.DESTROY:
        this.dispatchEvent(new SafeEvent(WindowModalState.DESTROY));
        break;
    }
  }
  get state() {
    return this.#state;
  }
  private isState(...states: WindowModalState[]) {
    for (const state of states) {
      if (state === this.state) {
        return true;
      }
    }
    return false;
  }
  protected awaitState(waitState: WindowModalState) {
    if (this.state === waitState) {
      return;
    }
    const waiter = new PromiseOut<void>();
    // deno-lint-ignore no-this-alias
    const self = this;
    this.addEventListener("statechange", function statechange(event) {
      if (event.state === waitState) {
        waiter.resolve();
        self.removeEventListener("statechange", statechange);
      }
    });
    return waiter.promise;
  }
  get isDestroyed() {
    return this.isState(WindowModalState.DESTROYING, WindowModalState.DESTROY);
  }
  async open() {
    if (this.isDestroyed || this.isState(WindowModalState.OPENING, WindowModalState.OPEN)) {
      return;
    }
    this.setState(WindowModalState.OPENING);
    await this.plugin
      .fetchApi("/openModal", { pathPrefix: "window.sys.dweb", search: { modalId: this.modal.modalId } })
      .boolean();
    await this.awaitState(WindowModalState.OPEN);
  }
  async close() {
    if (this.isDestroyed || this.isState(WindowModalState.CLOSING, WindowModalState.CLOSE)) {
      return;
    }
    this.setState(WindowModalState.CLOSING);
    await this.plugin
      .fetchApi("/closeModal", { pathPrefix: "window.sys.dweb", search: { modalId: this.modal.modalId } })
      .boolean();
    await this.awaitState(WindowModalState.CLOSE);
  }
  async destroy() {
    if (this.isDestroyed) {
      return;
    }
    this.setState(WindowModalState.DESTROYING);
    await this.plugin
      .fetchApi("/removeModal", { pathPrefix: "window.sys.dweb", search: { modalId: this.modal.modalId } })
      .boolean();
    await this.awaitState(WindowModalState.DESTROY);
  }
}
