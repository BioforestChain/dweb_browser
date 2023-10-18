import { SafeEvent, SafeEventTarget } from "../../helper/SafeEventTarget.ts";
import { windowPlugin } from "./window.plugin.ts";
import { $BottomSheetsModal } from "./window.type.ts";

export const enum BottomSheetsState {
  INIT = "init",
  SHOWING = "opening",
  SHOW = "open",
  HIDING = "hiding",
  HIDE = "hide",
  CLOSING = "closing",
  CLOSE = "close",
}
export class BottomSheets extends SafeEventTarget<{
  show: SafeEvent<"show">;
  hide: SafeEvent<"hide">;
  close: SafeEvent<"close">;
}> {
  constructor(readonly modal: $BottomSheetsModal, onDismiss: Promise<void>) {
    super();
    onDismiss.finally(() => {
      if (
        this.isState(
          BottomSheetsState.HIDE,
          BottomSheetsState.HIDING,
          BottomSheetsState.CLOSING,
          BottomSheetsState.CLOSE
        )
      ) {
        return;
      }
      this.state = BottomSheetsState.HIDE;
    });
  }
  #state = BottomSheetsState.INIT;
  get state() {
    return this.#state;
  }
  private set state(state: BottomSheetsState) {
    if (this.#state === state) {
      return;
    }
    this.#state = state;
    switch (state) {
      case BottomSheetsState.SHOW:
        this.dispatchEvent(new SafeEvent("show"));
        break;
      case BottomSheetsState.HIDE:
        this.dispatchEvent(new SafeEvent("hide"));
        break;
      case BottomSheetsState.CLOSE:
        this.dispatchEvent(new SafeEvent("close"));
        break;
    }
  }
  private isState(...states: BottomSheetsState[]) {
    for (const state of states) {
      if (state === this.state) {
        return true;
      }
    }
    return false;
  }
  async show() {
    if (this.isState(BottomSheetsState.SHOW, BottomSheetsState.SHOWING)) {
      return;
    }
    if (this.isState(BottomSheetsState.CLOSING, BottomSheetsState.CLOSE)) {
      return;
    }
    this.state = BottomSheetsState.SHOWING;
    await windowPlugin.fetchApi("/openModal", { search: { modalId: this.modal.modalId } }).boolean();
    this.state = BottomSheetsState.SHOW;
  }
  async hide() {
    if (this.isState(BottomSheetsState.HIDE, BottomSheetsState.HIDING)) {
      return;
    }
    if (this.isState(BottomSheetsState.CLOSING, BottomSheetsState.CLOSE)) {
      return;
    }
    this.state = BottomSheetsState.HIDING;
    await windowPlugin.fetchApi("/closeModal", { search: { modalId: this.modal.modalId } }).boolean();
    this.state = BottomSheetsState.HIDE;
  }
  async close() {
    if (this.isState(BottomSheetsState.CLOSING, BottomSheetsState.CLOSE)) {
      return;
    }
    this.state = BottomSheetsState.CLOSING;
    await windowPlugin.fetchApi("/removeModal", { search: { modalId: this.modal.modalId } }).boolean();
    this.dispatchEvent(new SafeEvent("close"));
    this.state = BottomSheetsState.CLOSE;
  }
}
