import { bindThis } from "../../helper/bindThis.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { $AgbaColor } from "../../util/color.ts";
import { $Insets, DOMInsets } from "../../util/insets.ts";
import { $Coder, StateObserver } from "../../util/StateObserver.ts";
import { BasePlugin } from "./BasePlugin.ts";
import { $InsetsRawState, $InsetsState, InsetsPlugin } from "./InsetsPlugin.ts";

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
   * 设置状态栏背景色
   * @param r 0~255
   * @param g 0~255
   * @param b 0~255
   * @param a 0~1
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
   * 设置状态栏风格
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
   * 显示状态栏。
   * 在 iOS 上，如果状态栏最初是隐藏的，并且初始样式设置为
   * `UIStatusBarStyleLightContent`，第一次显示调用可能会在
   * 动画将文本显示为深色然后过渡为浅色。 值得推荐
   * 在第一次调用时使用 `Animation.None` 作为动画。
   *
   * @since 1.0.0
   */
  @bindThis
  show() {
    return this.setVisible(true);
  }

  /**
   * Hide the bar.
   *
   * @since 1.0.0
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
   * 设置状态栏是否应该覆盖 webview 以允许使用
   * 它下面的空间。
   *
   */
  @bindThis
  setOverlay(overlay: boolean) {
    return this.setStateByKey("overlay", overlay);
  }
  @bindThis
  async getOverlay() {
    return (await this.state.getState()).overlay;
  }
}
