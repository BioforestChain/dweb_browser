import { BasePlugin } from "../basePlugin.ts";
import { AnimationOptions, BackgroundColorOptions, SetOverlaysWebViewOptions, StatusBarInfo, StyleOptions } from "./statusbar.type.ts";
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
export class StatusbarPlugin extends BasePlugin {

    constructor(readonly mmid = "statusBar.sys.dweb") {
        super(mmid, "StatusBar")
    }

    /**
     * 设置状态栏背景色
     * @param r 0~255
     * @param g 0~255
     * @param b 0~255
     * @param a 0~1
     */
    async setBackgroundColor(options: BackgroundColorOptions): Promise<void> {

    }
    // 支持 light | dark | defalt
    async setStyle(style: StyleOptions) {

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
    async show(options?: AnimationOptions): Promise<void> {

    }

    /**
     * Hide the status bar.
     *
     * @since 1.0.0
     */
    async hide(options?: AnimationOptions): Promise<void> {

    }

    /**
    * 获取有关状态栏当前状态的信息。
    *
    * @since 1.0.0
    */
    // async getInfo(): Promise<StatusBarInfo> {
    //     return { visible :}
    // }

    /**
    * 设置状态栏是否应该覆盖 webview 以允许使用
    * 它下面的空间。
    *
    * 此方法仅在 Android 上支持。
    *
    * @since 1.0.0
    */
    async setOverlaysWebView(options: SetOverlaysWebViewOptions): Promise<void> {

    }

}

