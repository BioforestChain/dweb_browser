import { bindThis } from "../../helper/bindThis.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { domInsetsToJson, insetsToDom } from "../../util/insets.ts";
import { $Coder, StateObserver } from "../../util/StateObserver.ts";
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
}

export const virtualKeyboardPlugin = new VirtualKeyboardPlugin();
