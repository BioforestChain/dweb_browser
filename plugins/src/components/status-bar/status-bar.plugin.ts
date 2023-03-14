import { bindThis } from "../../helper/bindThis.ts";
import {
  COLOR_FORMAT,
  convertColor,
  normalizeColor,
} from "../../helper/color.ts";
import { BasePlugin } from "../basePlugin.ts";
import {
  type AnimationOptions,
  type BackgroundColorOptions,
  type IStatusBarPlugin,
  type SetOverlaysWebViewOptions,
  EStatusBarAnimation,
  type StatusBarInfo,
  type StyleOptions,
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

  // mmid 最好全部采用小写，防止出现不可预期的意外
  constructor(readonly mmid = "statusbar.sys.dweb") {
    super("statusbar.sys.dweb");
  }

  startObserve() {
    return this.fetchApi(`/startObserve`);
  }

  observe() {
    return this.buildInternalRequest("/observe", { search: this.mmid })
      .fetch()
      .jsonlines<StatusBarInfo>();
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
    return normalizeColor(
      (await this._getCurrentInfo()).color,
      COLOR_FORMAT.HEXA
    );
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
    const animation = options?.animation ?? EStatusBarAnimation.None;
    await this.fetchApi(`/setVisible?visible=true&animation=${animation}`);
  }

  /**
   * Hide the status bar.
   *
   * @since 1.0.0
   */
  @bindThis
  async hide(options?: AnimationOptions): Promise<void> {
    const animation = options?.animation ?? EStatusBarAnimation.None;
    await this.fetchApi(`/setVisible`, {
      search: {
        visible: false,
        animation: animation,
      },
    });
  }

  /**
   * 获取有关状态栏当前状态的信息。
   *
   * @since 1.0.0
   */
  @bindThis
  async getInfo() {
    return (this.currentInfo = await this.fetchApi(
      `/getInfo`
    ).object<StatusBarInfo>());
  }
  private _getCurrentInfo() {
    return this.currentInfo ?? this.getInfo();
  }
  /**
   * 当前的状态集合
   */
  currentInfo?: StatusBarInfo;

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
    return await this.fetchApi(`/getOverlay`).boolean();
  }
}
export const statusBarPlugin = new StatusBarPlugin();
