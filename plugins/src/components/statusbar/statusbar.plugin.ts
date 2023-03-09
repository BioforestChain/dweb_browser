import { convertToRGBAHex } from "../../helper/color.ts";
import { BasePlugin } from "../basePlugin.ts";
import { AnimationOptions, BackgroundColorOptions, IStatusBarPlugin, SetOverlaysWebViewOptions, EStatusBarAnimation, StatusBarInfo, StyleOptions, StatusbarStyle } from "./statusbar.type.ts";
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
export class StatusbarPlugin extends BasePlugin implements IStatusBarPlugin {

    private _visible: boolean = true;
    private _style: StatusbarStyle = StatusbarStyle.Default;
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
    async setBackgroundColor(options: BackgroundColorOptions): Promise<Response> {
        const colorHex = convertToRGBAHex(options.color ?? "");
        return await this.nativeFetch(`/setBackgroundColor?color=${colorHex}`)
    }
    /**
     *  获取背景颜色
     */
    async getBackgroundColor() {
        return await this.nativeFetch(`/getBackgroundColor`)
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
    async setStyle(styleOptions: StyleOptions) {
        await this.nativeFetch(`/setStyle?style=${styleOptions.style}`)
    }
    /**
     * 获取当前style
     * @returns 
     */
    async getStyle() {
        return (await this.getInfo()).style
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
        const animation = options?.animation ?? EStatusBarAnimation.None
        await this.nativeFetch(`/setVisible?visible=true&animation=${animation}`)
    }

    /**
     * Hide the status bar.
     *
     * @since 1.0.0
     */
    async hide(options?: AnimationOptions): Promise<void> {
        const animation = options?.animation ?? EStatusBarAnimation.None
        await this.nativeFetch(`/setVisible?visible=false&animation=${animation}`)
    }

    /**
    * 获取有关状态栏当前状态的信息。
    *
    * @since 1.0.0
    */
    async getInfo(): Promise<StatusBarInfo> {
        const result: StatusBarInfo = await this.nativeFetch(`/getInfo`).then(res => res.json()).catch(err => err)
        return result
    }

    /**
    * 设置状态栏是否应该覆盖 webview 以允许使用
    * 它下面的空间。
    *
    * 此方法仅在 Android 上支持。
    *
    * @since 1.0.0
    */
    async setOverlaysWebView(options: SetOverlaysWebViewOptions): Promise<void> {
        await this.nativeFetch(`/setOverlays?overlay=${options.overlay}`)
    }

}

