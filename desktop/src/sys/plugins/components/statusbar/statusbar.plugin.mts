import type {$StatusbarStyle, $isOverlays} from "../../../statusbar/statusbar.main.cjs"
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
class StatusbarPlugin extends HTMLElement{
    private _statusbarHttpAddress: string | undefined = "./operation_from_plugins"
    private _appUrl: string | undefined = undefined

    constructor(){
        super()
        this._appUrl = location.origin;
    }

    /**
     * 设置状态栏的颜色
     * @param color 
     * @returns 
     */
    async setBackgroundColor(color: string){
        // todo 需要把颜色转化为十六进制格式 #FFFF
        return this._set('set_background_color', color)
    }

    // 支持 light | dark | defalt
    async setStyle(style: $StatusbarStyle){
        if(style !== "light" && style !== "dark" && style !== "default") return console.error('设置状态栏style出错，非法的参数！')
        return this._set('set_style', style)
    }

    // 获取状态栏样式
    async getStyle(){
        return this._set('get_style', "")
    }

    // 获取statusbar的高度
    async getHeight(){
        return this._set('get_height', "")
    }

    /**
     * 设置状态栏是否覆盖
     * @param value 
     */
    setOverlaysWebview(value: $isOverlays){
        if(value !== "0" && value !== "1") throw new Error('setOverlaysWebview 非法的参数 参数范围 "0" | "1" 当前参数==='+value)
        return this._set('set_overlays', value)
    }

    getOverlaysWebview(){
        return this._set('get_overlays', "")
    }

    private async _set(action: string, value: string){
        if(this._statusbarHttpAddress === undefined) return console.error('this._statusbarHttpAddress === undefined')
        const result = await fetch(
            `${this._statusbarHttpAddress}?app_url=${this._appUrl}`, 
            {
                method: "PUT",
                body: JSON.stringify({action: action, value: value}),
                headers: {
                    "Content-Type": "application/json; charset=UTF-8",
                    "Plugin-Target": "statusbar"
                }
            }
        )
        return Promise.resolve(JSON.parse(await result.json()).value)
    }

};

customElements.define('statusbar-dweb', StatusbarPlugin)
 
// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded(){
    const el = new StatusbarPlugin();
    document.body.append(el);
    document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
};

