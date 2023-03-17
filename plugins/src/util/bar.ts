import { BasePlugin } from "../components/basePlugin.ts";
import { bindThis } from "../helper/bindThis.ts";
import { cacheGetter } from "../helper/cacheGetter.ts";
import { $Transform } from "../helper/JsonlinesStream.ts";
import { $AgbaColor } from "./color.ts";
import { $Insets, DOMInsets } from "./insets.ts";
import { $Rect } from "./rect.ts";
import { $Coder, StateObserver } from "./StateObserver.ts";

/**
 * 状态栏的风格
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
   * If the device is using Dark mode, the statusbar text will be light.
   * If the device is using Light mode, the statusbar text will be dark.
   * On Android the default will be the one the app was launched with.
   *
   */
  Default = "DEFAULT",
}
export interface $BarRawState {
  /**
   * Whether the status bar is visible or not.
   */
  visible: boolean;

  /**
   * The current status bar style.
   */
  style: BAR_STYLE;

  /**
   * The current status bar color.
   *
   * This option is only supported on Android.
   */
  color: $AgbaColor;

  /**
   * Whether the statusbar is overlaid or not.
   *
   * This option is only supported on Android.
   */
  overlay: boolean;
  insets: $Insets;
}

export interface $BarState {
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
> extends BasePlugin {
  abstract coder: $Coder<RAW, STATE>;

  @cacheGetter()
  get state() {
    return new StateObserver(
      this,
      () => this.fetchApi(`/getState`).object<RAW>(),
      this.coder
    );
  }

  abstract setStates(state: Partial<WRITABLE_STATE>): Promise<unknown>;
  abstract setState<K extends keyof WRITABLE_STATE>(
    key: K,
    value: WRITABLE_STATE[K]
  ): Promise<unknown>;

  /**
   * 设置状态栏背景色
   * @param r 0~255
   * @param g 0~255
   * @param b 0~255
   * @param a 0~1
   */
  @bindThis
  setColor(color: string) {
    return this.setState("color", color);
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
    return this.setState("style", style);
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
   * Hide the status bar.
   *
   * @since 1.0.0
   */
  @bindThis
  hide() {
    return this.setVisible(false);
  }
  @bindThis
  setVisible(visible: boolean) {
    return this.setState("visible", visible);
  }
  @bindThis
  async getVisible() {
    return (await this.state.getState()).visible;
  }

  /**
   * 设置状态栏是否应该覆盖 webview 以允许使用
   * 它下面的空间。
   *
   * 此方法仅在 Android 上支持。
   *
   * @since 1.0.0
   */
  @bindThis
  setOverlay(overlay: boolean) {
    return this.setState("overlay", overlay);
  }
  @bindThis
  async getOverlay() {
    return (await this.state.getState()).overlay;
  }
}
