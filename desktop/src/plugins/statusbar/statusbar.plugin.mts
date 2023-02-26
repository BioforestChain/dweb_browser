import type {$StatusbarStyle, $isOverlays} from "../../sys/statusbar/statusbar.main.cjs"
class StatusbarPlugin extends HTMLElement{
    // private _statusbarHttpAddress: string | undefined = "http://status.sys.dweb-80.localhost:22605/from_plugins"
    private _statusbarHttpAddress: string | undefined = "./operation"
    private _appUrl: string | undefined = undefined

    constructor(){
        super()
        this._appUrl = location.origin;
    }

    setBackgroundColor(color: string){
        // todo 需要把颜色转化为十六进制格式 #FFFF
        return this._set('set_background_color', color)
    }

    // 支持 light | dark | defalt
    setStyle(style: $StatusbarStyle){
        if(style !== "light" && style !== "dark" && style !== "default") return console.error('设置状态栏style出错，非法的参数！')
        return this._set('set_style', style)
    }

    getStyle( ){
        return fetch(
            `${this._statusbarHttpAddress}?app_url=${this._appUrl}`,
            {
                method: "PUT",
                body: JSON.stringify({action: "get_style", value: ""}),
                headers: {
                    "Content-Type": "application/json; charset=UTF-8"
                }
            } 
        )
    }

    setOverlaysWebview(value: $isOverlays){
        this._set('set_overlays', value)
    }

    private async _set(action: string, value: string){
        if(this._statusbarHttpAddress === undefined) return console.error('this._statusbarHttpAddress === undefined')
        return fetch(
            `${this._statusbarHttpAddress}?app_url=${this._appUrl}`, 
            {
                method: "PUT",
                body: JSON.stringify({action: action, value: value}),
                headers: {
                    "Content-Type": "application/json; charset=UTF-8"
                }
            }
        )
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

