import { bindThis } from "../../helper/bindThis.ts";
import {
  COLOR_FORMAT,
  convertColor,
  normalizeColor,
} from "../../helper/color.ts";
import { $Callback } from "../../helper/createSignal.ts";
import { BasePlugin } from "../basePlugin.ts";
import {
  type AnimationOptions,
  type BackgroundColorOptions,
  type IStatusBarPlugin,
  type SetOverlaysWebViewOptions,
  EStatusBarAnimation,
  type StatusBarRawInfo,
  type StyleOptions,
  StatusBarInfo,
} from "./status-bar.type.ts";
/**
 * 访问 statusbar 能力的插件
 *
 * @property setBackgroundColor(color: string): string;
 * @property setStyle(style: "light" | "dark" | "defalt"): "light" | "dark" | "defalt"
 * @property setOverlaysWebview(value: "0" | "1"): "0" | "1" {"0": 不覆盖, "1": 覆盖}
 * @property getStyle()："light" | "dark" | "defalt"
 * @property getHeight(): number
 * @property getOverlaysWebview(): "0" | "1"
 */
export class StatusBarPlugin extends BasePlugin implements IStatusBarPlugin {
  readonly tagName = "dweb-status-bar";
  // private _visible: boolean = true;
  // private _style: StatusbarStyle = StatusbarStyle.Default;
  // private _color: string = "";
  // private _overlays: boolean = false;

  constructor() {
    super("status-bar.sys.dweb");
  }

  startObserve() {
    return this.fetchApi(`/startObserve`);
  }

  async observe() {
    return this.buildInternalApiRequest("/observe", {
      search: { mmid: this.mmid },
      base: await BasePlugin.public_url,
    })
      .fetch()
      .jsonlines<StatusBarRawInfo>();
  }

  stopObserve() {
    return this.fetchApi(`/stopObserve`);
  }

  /**
   * 设置状态栏背景色
   * @param r 0~255
   * @param g 0~255
   * @param b 0~255
   * @param a 0~1
   */
  @bindThis
  async setBackgroundColor(options: BackgroundColorOptions) {
    const color = convertColor(options.color);
    await this.fetchApi(`/setBackgroundColor`, {
      search: color,
    });
  }
  /**
   *  获取背景颜色
   */
  @bindThis
  async getBackgroundColor() {
    return (await this.getInfo()).color;
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
  async setStyle(styleOptions: StyleOptions) {
    await this.fetchApi(`/setStyle`, {
      search: {
        style: styleOptions.style,
      },
    });
  }
  /**
   * 获取当前style
   * @returns
   */
  @bindThis
  async getStyle() {
    const result = await this.getInfo();
    return result.style;
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
  async show(options?: AnimationOptions): Promise<void> {
    return this.setVisible(true, options);
  }

  /**
   * Hide the status bar.
   *
   * @since 1.0.0
   */
  @bindThis
  async hide(options?: AnimationOptions): Promise<void> {
    return this.setVisible(false, options);
  }
  @bindThis
  async setVisible(visible: boolean, options?: AnimationOptions) {
    const animation = options?.animation ?? EStatusBarAnimation.None;
    await this.fetchApi(`/setVisible`, {
      search: {
        visible: visible,
        animation: animation,
      },
    });
  }
  @bindThis
  async getVisible() {
    await this.fetchApi(`/getVisible`);
  }

  /**
   * 刷新获取有关状态栏当前状态的信息。
   */
  private async _updateCurrentInfo() {
    const rawInfo = await this.fetchApi(`/getInfo`).object<StatusBarRawInfo>();
    return this.normalizeRawInfo(rawInfo);
  }
  normalizeRawInfo(rawInfo: StatusBarRawInfo) {
    const info: StatusBarInfo = {
      ...rawInfo,
      color: normalizeColor(rawInfo.color, COLOR_FORMAT.HEXA),
    };
    return info;
  }
  /**
   * 获取当前状态
   * @returns
   */
  @bindThis
  async getInfo(force_update = false) {
    if (force_update || this.currentInfo === undefined) {
      return await this._updateCurrentInfo();
    }
    return this.currentInfo;
  }
  /**
   * 当前的状态集合
   */
  private _currentInfo?: StatusBarInfo | undefined;
  public get currentInfo(): StatusBarInfo | undefined {
    return this._currentInfo;
  }
  public set currentInfo(value: StatusBarInfo | undefined) {
    this._currentInfo = value;
    if (value) {
      this._signalCurrentInfoChange.emit(value);
    }
  }

  private _signalCurrentInfoChange =
    this.createSignal<$Callback<[StatusBarInfo]>>();
  readonly onCurrentInfoChange = this._signalCurrentInfoChange.listen;

  /**
   * 设置状态栏是否应该覆盖 webview 以允许使用
   * 它下面的空间。
   *
   * 此方法仅在 Android 上支持。
   *
   * @since 1.0.0
   */
  @bindThis
  async setOverlaysWebView(options: SetOverlaysWebViewOptions): Promise<void> {
    await this.fetchApi(`/setOverlay`, {
      search: {
        overlay: options.overlay,
      },
    });
  }
  @bindThis
  async getOverlaysWebView(): Promise<boolean> {
    return (await this.getInfo()).overlay;
  }
}
export const statusBarPlugin = new StatusBarPlugin();
