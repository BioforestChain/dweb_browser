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
class StatusbarPlugin extends BasePlugin {
    private _statusbarHttpAddress: string | undefined = "./operation_from_plugins"
    private _appUrl: string | undefined = undefined

    constructor(readonly mmid = "file://statusBar.sys.dweb") {
        super(mmid, "StatusBar")
        this._appUrl = location.origin;
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
      * Show the status bar.
      * On iOS, if the status bar is initially hidden and the initial style is set to
      * `UIStatusBarStyleLightContent`, first show call might present a glitch on the
      * animation showing the text as dark and then transition to light. It's recommended
      * to use `Animation.None` as the animation on the first call.
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

customElements.define('statusbar-dweb', StatusbarPlugin)

// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
    const el = new StatusbarPlugin();
    document.body.append(el);
    document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
}

