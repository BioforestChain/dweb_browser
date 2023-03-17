import { bindThis } from "../../helper/bindThis.ts";
import { $BarRawState, $BarState, BarPlugin } from "../../util/bar.ts";
import {
  COLOR_FORMAT,
  convertColorToArga,
  normalizeArgaToColor,
} from "../../util/color.ts";
import { insetsToDom, domInsetsToJson } from "../../util/insets.ts";
import { $Coder } from "../../util/StateObserver.ts";
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
  readonly tagName = "dweb-navigation-bar";

  constructor() {
    super("navigation-bar.sys.dweb");
  }
  coder: $Coder<$BarRawState, $BarState> = {
    decode: (raw: $NavigationBarRawState) => ({
      ...raw,
      color: normalizeArgaToColor(raw.color, COLOR_FORMAT.HEXA),
      insets: insetsToDom(raw.insets),
    }),
    encode: (state: $NavigationBarState) => ({
      ...state,
      color: convertColorToArga(state.color),
      insets: domInsetsToJson(state.insets),
    }),
  };

  @bindThis
  async setStates(state: Partial<$NavigationBarWritableState>) {
    await this.fetchApi("/setState", {
      search: {
        ...state,
        color: state.color ? convertColorToArga(state.color) : undefined,
      },
    });
  }
  @bindThis
  setState<K extends keyof $NavigationBarWritableState>(
    key: K,
    value: $NavigationBarWritableState[K]
  ) {
    return this.setStates({
      [key]: value,
    });
  }
  get getState() {
    return this.state.getState;
  }
}
export const navigationBarPlugin = new NavigationBarPlugin();
