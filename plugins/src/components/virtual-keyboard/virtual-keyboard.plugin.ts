import { bindThis } from "../../helper/bindThis.ts";
import { $Coder } from "../../util/StateObserver.ts";
import { InsetsPlugin } from "../base/InsetsPlugin.ts";
import {
  $VirtualKeyboardRawState,
  $VirtualKeyboardState,
  $VirtualKeyboardWritableState,
} from "./virtual-keyboard.type.ts";

export class VirtualKeyboardPlugin extends InsetsPlugin<
  $VirtualKeyboardRawState,
  $VirtualKeyboardState,
  $VirtualKeyboardWritableState
> {
  readonly tagName = "dweb-virtual-keyboard";
  constructor() {
    super("virtual-keyboard.nativeui.sys.dweb");
  }

  readonly coder: $Coder<$VirtualKeyboardRawState, $VirtualKeyboardState> =
    this.baseCoder;

  @bindThis
  async setState(state: Partial<$VirtualKeyboardWritableState>) {
    await this.commonSetState(state);
  }
  @bindThis
  setStateByKey<K extends keyof $VirtualKeyboardWritableState>(
    key: K,
    value: $VirtualKeyboardWritableState[K]
  ) {
    return this.setState({ [key]: value });
  }
  get getState() {
    return this.state.getState;
  }
  @bindThis
  setOverlay(overlay: boolean) {
    return this.setStateByKey("overlay", overlay);
  }
  @bindThis
  async getOverlay() {
    return (await this.state.getState()).overlay;
  }
  @bindThis
  async getVisible() {
    return (await this.getState()).visible;
  }
  @bindThis
  setVisible(visible: boolean) {
    return this.setStateByKey("visible", visible);
  }
  /**
   * 显示虚拟键盘
   */
  @bindThis
  show() {
    return this.setVisible(true);
  }

  /**
   * 隐藏虚拟键盘
   */
  @bindThis
  hide() {
    return this.setVisible(false);
  }
}

export const virtualKeyboardPlugin = new VirtualKeyboardPlugin();
