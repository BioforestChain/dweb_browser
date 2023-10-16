import { bindThis } from "../../helper/bindThis.ts";
import { DOMInsets } from "../../util/insets.ts";
import { BAR_STYLE } from "../base/BarPlugin.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { windowPlugin } from "../index.ts";
import { $StatusBarState, $StatusBarWritableState } from "./status-bar.type.ts";
/**
 * 访问 status-bar 能力的插件
 */
export class StatusBarPlugin extends BasePlugin {
  constructor() {
    super("window.sys.dweb");
  }

  @bindThis
  async setState(state: Partial<$StatusBarWritableState>) {
    let topBarContentColor: string | undefined = undefined;
    switch (state.style) {
      case BAR_STYLE.Dark:
        topBarContentColor = "#000000";
        break;
      case BAR_STYLE.Light:
        topBarContentColor = "#FFFFFF";
        break;
      default:
        topBarContentColor = "auto";
    }
    windowPlugin.setStyle({
      topBarBackgroundColor: state.color,
      topBarContentColor,
      topBarOverlay: state.overlay,
    });
  }
  @bindThis
  setStateByKey<K extends keyof $StatusBarWritableState>(key: K, value: $StatusBarWritableState[K]) {
    return this.setState({
      [key]: value,
    });
  }
  async getState() {
    const winState = await windowPlugin.getState();
    let style = BAR_STYLE.Default;
    switch (winState.topBarContentColor) {
      case "#FFFFFF":
        style = BAR_STYLE.Light;
        break;
      case "#000000":
        style = BAR_STYLE.Dark;
        break;
    }
    return {
      color: winState.topBarBackgroundColor,
      style,
      overlay: winState.topBarOverlay,
      visible: true,
      insets: new DOMInsets(0, 0, 0, 0),
    } satisfies $StatusBarState;
  }
}
export const statusBarPlugin = new StatusBarPlugin();
