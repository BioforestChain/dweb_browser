import { bindThis } from "../../helper/bindThis.ts";
import type { $AgbaColor } from "../../util/color.ts";
import type { $Insets, DOMInsets } from "../../util/insets.ts";
import { InsetsPlugin, type $InsetsRawState, type $InsetsState } from "./insets.plugin.ts";

/**
 * 条栏的风格
 * Light 代表文字为黑色
 * Dark 代表文字为白色
 */
export enum BAR_STYLE {
  /**
   * Light text for dark backgrounds.
   *
   */
  Dark = "DARK",

  /**
   * Dark text for light backgrounds.
   *
   */
  Light = "LIGHT",

  /**
   * The style is based on the device appearance.
   * If the device is using Dark mode, the bar text will be light.
   * If the device is using Light mode, the bar text will be dark.
   * On Android the default will be the one the app was launched with.
   *
   */
  Default = "DEFAULT",
}
export interface $BarRawState extends $InsetsRawState {
  /**
   * Whether the bar is visible or not.
   */
  visible: boolean;

  /**
   * The current bar style.
   */
  style: BAR_STYLE;

  /**
   * The current bar color.
   *
   * This option is only supported on Android.
   */
  color: $AgbaColor;

  /**
   * Whether the bar is overlaid or not.
   *
   * This option is only supported on Android.
   */
  overlay: boolean;
  insets: $Insets;
}

export interface $BarState extends $InsetsState {
  color: string;
  style: BAR_STYLE;
  overlay: boolean;
  visible: boolean;
  insets: DOMInsets;
}
export type $BarWritableState = Omit<$BarState, "insets">;

export abstract class BarPlugin<
  RAW extends $BarRawState,
  STATE extends $BarState,
  WRITABLE_STATE extends $BarWritableState
> extends InsetsPlugin<RAW, STATE, WRITABLE_STATE> {
  /**
   * 设置 Bar 背景色
   * Hex
   */
  @bindThis
  setColor(color: string) {
    return this.setStateByKey("color", color);
  }
  /**
   *  获取背景颜色
   */
  @bindThis
  async getColor() {
    return (await this.state.getState()).color;
  }

  /**
   * 设置 Bar 风格
   * // 支持 light | dark | defalt
   * 据观测
   * 在系统主题为 Light 的时候, Default 意味着 白色字体
   * 在系统主题为 Dark 的手, Default 因为这 黑色字体
   * 这兴许与设置有关系, 无论如何, 尽可能避免使用 Default 带来的不确定性
   *
   * @param style
   */
  @bindThis
  setStyle(style: STATE["style"]) {
    return this.setStateByKey("style", style);
  }
  /**
   * 获取当前style
   * @returns
   */
  @bindThis
  async getStyle() {
    return (await this.state.getState()).style;
  }

  /**
   * 显示 Bar
   */
  @bindThis
  show() {
    return this.setVisible(true);
  }

  /**
   * Hide the bar.
   */
  @bindThis
  hide() {
    return this.setVisible(false);
  }
  @bindThis
  setVisible(visible: boolean) {
    return this.setStateByKey("visible", visible);
  }
  @bindThis
  async getVisible() {
    return (await this.state.getState()).visible;
  }

  /**
   * 设置 Bar 是否应该覆盖 webview 以允许使用
   * 它下面的空间。
   *
   */
  @bindThis
  override setOverlay(overlay: boolean) {
    return this.setStateByKey("overlay", overlay);
  }
  @bindThis
  override async getOverlay() {
    return (await this.state.getState()).overlay;
  }
}
