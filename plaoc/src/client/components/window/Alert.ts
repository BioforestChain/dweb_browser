import { SafeEvent, SafeEventTarget, SafeMessageEvent } from "../../helper/SafeEventTarget.ts";
import { windowPlugin } from "./window.plugin.ts";
import { $AlertModal } from "./window.type.ts";

export const enum AlertState {
  OPENING = "opening",
  OPEN = "open",
  CLOSING = "closing",
  CLOSE = "close",
}
export class Alert extends SafeEventTarget<{ result: SafeMessageEvent<"result", boolean>; close: SafeEvent<"close"> }> {
  constructor(readonly modal: $AlertModal, onResult: Promise<boolean>) {
    super();
    onResult.then((result) => {
      if (this.isState(AlertState.CLOSING, AlertState.CLOSE)) {
        return;
      }
      this.state = AlertState.CLOSE;
      void this._remove();
      this.dispatchEvent(new SafeMessageEvent("result", { data: result }));
    });
  }
  state = AlertState.OPEN;

  private isState(...states: AlertState[]) {
    for (const state of states) {
      if (state === this.state) {
        return true;
      }
    }
    return false;
  }
  async close() {
    if (this.isState(AlertState.CLOSING, AlertState.CLOSE)) {
      return;
    }
    this.state = AlertState.CLOSING;
    await this._remove();
    this.state = AlertState.CLOSE;
  }
  private async _remove() {
    await windowPlugin.fetchApi("/removeModal", { search: { modalId: this.modal.modalId } }).boolean();
    this.dispatchEvent(new SafeEvent("close"));
  }
}
