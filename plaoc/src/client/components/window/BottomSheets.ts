import { SafeEvent, SafeEventTarget } from "../../helper/SafeEventTarget.ts";
import { windowPlugin } from "./window.plugin.ts";
import { $BottomSheetsModal } from "./window.type.ts";

export const enum BottomSheetsState {
  INIT = "init",
  OPENING = "opening",
  OPEN = "open",
  CLOSING = "closing",
  CLOSE = "close",
  DESTROYING = "destroying",
  DESTROY = "destroy",
}
export class BottomSheets extends SafeEventTarget<{
  open: SafeEvent<BottomSheetsState.OPEN>;
  close: SafeEvent<BottomSheetsState.CLOSE>;
  destroy: SafeEvent<BottomSheetsState.DESTROY>;
}> {
  constructor(readonly modal: $BottomSheetsModal, onDismiss: Promise<void>) {
    super();
    onDismiss.finally(() => {
      if (
        this.isState(
          BottomSheetsState.CLOSE,
          BottomSheetsState.CLOSING,
          BottomSheetsState.DESTROYING,
          BottomSheetsState.DESTROY
        )
      ) {
        return;
      }
      this.state = BottomSheetsState.CLOSE;
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
      case BottomSheetsState.OPEN:
        this.dispatchEvent(new SafeEvent("show"));
        break;
      case BottomSheetsState.CLOSE:
        this.dispatchEvent(new SafeEvent("hide"));
        break;
      case BottomSheetsState.DESTROY:
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
    if (this.isState(BottomSheetsState.OPEN, BottomSheetsState.OPENING)) {
      return;
    }
    if (this.isState(BottomSheetsState.DESTROYING, BottomSheetsState.DESTROY)) {
      return;
    }
    this.state = BottomSheetsState.OPENING;
    await windowPlugin.fetchApi("/openModal", { search: { modalId: this.modal.modalId } }).boolean();
    this.state = BottomSheetsState.OPEN;
  }
  async hide() {
    if (this.isState(BottomSheetsState.CLOSE, BottomSheetsState.CLOSING)) {
      return;
    }
    if (this.isState(BottomSheetsState.DESTROYING, BottomSheetsState.DESTROY)) {
      return;
    }
    this.state = BottomSheetsState.CLOSING;
    await windowPlugin.fetchApi("/closeModal", { search: { modalId: this.modal.modalId } }).boolean();
    this.state = BottomSheetsState.CLOSE;
  }
  async close() {
    if (this.isState(BottomSheetsState.DESTROYING, BottomSheetsState.DESTROY)) {
      return;
    }
    this.state = BottomSheetsState.DESTROYING;
    await windowPlugin.fetchApi("/removeModal", { search: { modalId: this.modal.modalId } }).boolean();
    this.dispatchEvent(new SafeEvent("close"));
    this.state = BottomSheetsState.DESTROY;
  }
}
