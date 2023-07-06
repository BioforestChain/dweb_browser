import { bindThis } from "../../helper/bindThis.ts";
import {
  COLOR_FORMAT,
  convertColorToArga,
  normalizeArgaToColor,
} from "../../util/color.ts";
import { domInsetsToJson } from "../../util/insets.ts";
import { $Coder } from "../../util/StateObserver.ts";
import { BarPlugin } from "../base/BarPlugin.ts";
import {
  $NavigationBarWritableState,
  type $NavigationBarRawState,
  type $NavigationBarState,
} from "./navigation-bar.type.ts";
/**
 * 访问 navigation-bar 能力的插件
 */
export class NavigationBarPlugin extends BarPlugin<
  $NavigationBarRawState,
  $NavigationBarState,
  $NavigationBarWritableState
> {
  constructor() {
    super("navigation-bar.nativeui.browser.dweb");
  }
  coder: $Coder<$NavigationBarRawState, $NavigationBarState> = {
    decode: (raw) => ({
      ...this.baseCoder.decode(raw),
      color: normalizeArgaToColor(raw.color, COLOR_FORMAT.HEXA),
    }),
    encode: (state) => ({
      ...state,
      color: convertColorToArga(state.color),
      insets: domInsetsToJson(state.insets),
    }),
  };

  @bindThis
  async setState(state: Partial<$NavigationBarWritableState>) {
    await this.commonSetState({
      ...state,
      color: state.color ? convertColorToArga(state.color) : undefined,
    });
  }
  @bindThis
  setStateByKey<K extends keyof $NavigationBarWritableState>(
    key: K,
    value: $NavigationBarWritableState[K]
  ) {
    return this.setState({
      [key]: value,
    });
  }
  override get getState() {
    return this.state.getState;
  }
}
export const navigationBarPlugin = new NavigationBarPlugin();
