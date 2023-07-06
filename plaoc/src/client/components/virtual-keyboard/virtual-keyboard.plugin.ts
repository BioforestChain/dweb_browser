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
  constructor() {
    super("virtual-keyboard.nativeui.browser.dweb");
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
  override get getState() {
    return this.state.getState;
  }
  @bindThis
  override setOverlay(overlay: boolean) {
    return this.setStateByKey("overlay", overlay);
  }
  @bindThis
  override async getOverlay() {
    return (await this.state.getState()).overlay;
  }
}

export const virtualKeyboardPlugin = new VirtualKeyboardPlugin();
