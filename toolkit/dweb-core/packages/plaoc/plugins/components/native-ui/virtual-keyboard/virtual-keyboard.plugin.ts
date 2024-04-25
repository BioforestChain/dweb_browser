import { bindThis } from "../../../helper/bindThis.ts";
import { DOMInsets } from "../../../util/insets.ts";
import { BasePlugin } from "../../base/base.plugin.ts";
import { windowPlugin } from "../../window/window.plugin.ts";
import { $VirtualKeyboardWritableState } from "./virtual-keyboard.type.ts";

export class VirtualKeyboardPlugin extends BasePlugin {
  constructor() {
    super("window.sys.dweb");
  }

  @bindThis
  async setState(state: Partial<$VirtualKeyboardWritableState>) {
    await windowPlugin.setStyle({
      keyboardOverlaysContent: state.overlay,
    });
  }
  @bindThis
  setStateByKey<K extends keyof $VirtualKeyboardWritableState>(key: K, value: $VirtualKeyboardWritableState[K]) {
    return this.setState({ [key]: value });
  }
  @bindThis
  async getState() {
    const winState = await windowPlugin.getState();
    return {
      overlay: winState.keyboardOverlaysContent,
      insets: new DOMInsets(0, 0, 0, 0),
    };
  }
  @bindThis
  setOverlay(overlay: boolean) {
    return this.setStateByKey("overlay", overlay);
  }
  @bindThis
  async getOverlay() {
    return (await this.getState()).overlay;
  }
}

export const virtualKeyboardPlugin = new VirtualKeyboardPlugin();
