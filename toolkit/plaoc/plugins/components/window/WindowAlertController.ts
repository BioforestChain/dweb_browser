import type { $Callback, Signal } from "@dweb-browser/helper/createSignal.ts";
import type { BasePlugin } from "../base/base.plugin.ts";
import { WindowModalController } from "./WindowModalController.ts";
import type { $AlertModal, $ModalCallback } from "./window.type.ts";

export class WindowAlertController extends WindowModalController {
  constructor(plugin: BasePlugin, modal: $AlertModal, onCallback: Signal<$Callback<[$ModalCallback]>>) {
    super(plugin, modal, onCallback);
    onCallback.listen((data) => {
      if (data.type === "close-alert") {
        this.#result = data.confirm;
      }
      if (data.type === "open") {
        this.#result = false;
      }
    });
  }
  #result = false;
  get result() {
    return this.#result;
  }
}
