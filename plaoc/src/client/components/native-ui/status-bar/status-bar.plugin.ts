import { bindThis } from "../../../helper/bindThis.ts";
import { DOMInsets } from "../../../util/insets.ts";
import { BAR_STYLE } from "../../base/bar.plugin.ts";
import { BasePlugin } from "../../base/base.plugin.ts";
import { windowPlugin } from "../../index.ts";
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
  @bindThis
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
  @bindThis
  setColor(color: string) {
    return this.setStateByKey("color", color);
  }
  @bindThis
  async getColor() {
    return (await this.getState()).color;
  }
  @bindThis
  setStyle(style: BAR_STYLE) {
    return this.setState({ style });
  }
  @bindThis
  async getStyle() {
    return (await this.getState()).style;
  }
  @bindThis
  setOverlay(overlay: boolean) {
    return this.setState({ overlay });
  }
  @bindThis
  async getOverlay() {
    return (await this.getState()).overlay;
  }
  @bindThis
  setVisible(visible: boolean) {
    return this.setState({ visible });
  }
  @bindThis
  async getVisible() {
    return (await this.getState()).visible;
  }
  @bindThis
  async show(){}
  @bindThis
  async hide(){}
}
export const statusBarPlugin = new StatusBarPlugin();
