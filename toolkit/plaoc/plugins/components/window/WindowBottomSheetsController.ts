import type { $Callback, Signal } from "@dweb-browser/helper/createSignal.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import { WindowModalController } from "./WindowModalController.ts";
import type { $BottomSheetsModal, $ModalCallback } from "./window.type.ts";

export class WindowBottomSheetsController extends WindowModalController {
  constructor(plugin: BasePlugin, modal: $BottomSheetsModal, onCallback: Signal<$Callback<[$ModalCallback]>>) {
    super(plugin, modal, onCallback);
  }
}
