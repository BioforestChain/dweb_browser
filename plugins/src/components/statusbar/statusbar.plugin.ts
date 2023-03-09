import { BasePlugin } from "../basePlugin.ts";
import { AnimationOptions, BackgroundColorOptions, SetOverlaysWebViewOptions, StatusBarInfo, StyleOptions, Style } from "./statusbar.type.ts";
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

    private _visible: boolean = true;
    private _style: Style = Style.Default ;
    private _color: string = "";
    private _overlays: boolean = false;

    // mmid 最好全部采用小写，防止出现不可预期的意外
    constructor(readonly mmid = "statusbar.sys.dweb") {
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
    // 支持 LIGHT | DARK | DEFAULT
    async setStyle(styleOptions: StyleOptions) {
        const request = new Request(
            `/api?app_id=${this.mmid}&action=set_style&value=${styleOptions.style}`, 
            { 
                method: "PUT",
                headers: {
                    "Content-type": "application/json"
                }
            }
        )

        return this.nativeFetch(request)
                .then(res => {
                    if(res.status === 200){ /** 如果成功 保存状态 */
                        this._style = styleOptions.style;
                    }
                    return res;
                })
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

