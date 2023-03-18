import { bindThis } from "../../helper/bindThis.ts";
import {
  COLOR_FORMAT,
  convertColorToArga,
  normalizeArgaToColor,
} from "../../util/color.ts";
import { $Coder } from "../../util/StateObserver.ts";
import { BarPlugin } from "../base/BarPlugin.ts";
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
    super("status-bar.nativeui.sys.dweb");
  }
  coder: $Coder<$StatusBarRawState, $StatusBarState> = {
    decode: (raw: $StatusBarRawState) => ({
      ...this.baseCoder.decode(raw),
      color: normalizeArgaToColor(raw.color, COLOR_FORMAT.HEXA),
    }),
    encode: (state: $StatusBarState) => ({
      ...this.baseCoder.encode(state),
      color: convertColorToArga(state.color),
    }),
  };

  @bindThis
  async setState(state: Partial<$StatusBarWritableState>) {
    await this.commonSetState({
      ...state,
      color: state.color ? convertColorToArga(state.color) : undefined,
    });
  }
  @bindThis
  setStateByKey<K extends keyof $StatusBarWritableState>(
    key: K,
    value: $StatusBarWritableState[K]
  ) {
    return this.setState({
      [key]: value,
    });
  }
  get getState() {
    return this.state.getState;
  }
}
export const statusBarPlugin = new StatusBarPlugin();
