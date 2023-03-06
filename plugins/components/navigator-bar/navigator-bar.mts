// navigator-bar 插件 
import type { $NavigatorbarItemContent } from "../../../navigator-bar/navigator-bar.cjs";

class Navigatorbar extends HTMLElement{
    private _navigatorbarHttpAddress: string | undefined = "./operation_from_plugins"
    private _appUrl: string | undefined = undefined

    constructor(){
        super()
        this._appUrl = location.origin;
    }

    connectedCallback(){
        // 发起一个监听请求
        this._addClickNavigatorbarItem()
    }

    /**
     * 设置 navigatorbar 的内容
     * @param arr 
     * @returns 
     */
    setNavigatorbarContent(arr: $NavigatorbarItemContent[]){
        return this._set('set_content', JSON.stringify(arr))
    }

    show(){
        return this._set("show", "")
    }

    hide(){
        return this._set("hide", "")
    }

    private _addClickNavigatorbarItem = async () => {
        console.log('发起了一个监听点击事件')
        const result = await this._set("listener_click", "")
        if(result === null) return console.error('监听操作失败')
        this.dispatchEvent(new CustomEvent("click-item", {cancelable: true, detail: result}))
        this._addClickNavigatorbarItem()   
    }



    async _set(action: string, value: string){
        if(this._navigatorbarHttpAddress === undefined) return console.error('this._navigatorbarHttpAddress === undefined')
        const result = await fetch(
            `${this._navigatorbarHttpAddress}?app_url=${this._appUrl}`, 
            {
                method: "PUT",
                body: JSON.stringify({action: action, value: value}),
                headers: {
                    "Content-Type": "application/json; charset=UTF-8",
                    "Plugin-Target": "navigatorbar"
                }
            }
        )
        // 如果返回的 value === null 表示出错了
        return Promise.resolve(JSON.parse(await result.json()).value)
    }
}

customElements.define('navigatorbar-dweb', Navigatorbar);

// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded(){
    const el = new Navigatorbar();
    document.body.append(el);
    document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
};
