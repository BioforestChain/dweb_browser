import { bindThis } from "../../helper/bindThis.ts";
import { $BarRawState, $BarState, BarPlugin } from "../../util/bar.ts";
import {
  COLOR_FORMAT,
  convertColorToArga,
  normalizeArgaToColor,
} from "../../util/color.ts";
import { $Coder } from "../../util/StateObserver.ts";
import {
  $StatusBarWritableState,
  type $StatusBarRawState,
  type $StatusBarState,
} from "./status-bar.type.ts";
/**
 * 访问 status-bar 能力的插件
 */
export class StatusBarPlugin extends BarPlugin<
  $StatusBarRawState,
  $StatusBarState,
  $StatusBarWritableState
> {
  readonly tagName = "dweb-status-bar";

  constructor() {
    super("status-bar.sys.dweb");
  }
  coder: $Coder<$BarRawState, $BarState> = {
    decode: (raw: $StatusBarRawState) => ({
      ...raw,
      color: normalizeArgaToColor(raw.color, COLOR_FORMAT.HEXA),
    }),
    encode: (state: $StatusBarState) => ({
      ...state,
      color: convertColorToArga(state.color),
    }),
  };

  @bindThis
  async setStates(state: Partial<$StatusBarWritableState>) {
    await this.fetchApi("/setState", {
      search: {
        ...state,
        color: state.color ? convertColorToArga(state.color) : undefined,
      },
    });
  }
  @bindThis
  setState<K extends keyof $StatusBarWritableState>(
    key: K,
    value: $StatusBarWritableState[K]
  ) {
    return this.setStates({
      [key]: value,
    });
  }
  get getState() {
    return this.state.getState;
  }
}
export const statusBarPlugin = new StatusBarPlugin();
