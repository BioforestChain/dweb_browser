import { BasePlugin, PluginListenerHandle } from '../../basePlugin.ts';
import { NavigationBarPluginEvents } from "./navigator.events.ts";
import { ColorParameters } from "./navigator.type.ts"
// navigator-bar 插件 
class Navigatorbar extends BasePlugin {
    private _navigatorbarHttpAddress: string | undefined = "./operation_from_plugins"
    private _appUrl: string | undefined = undefined

    constructor(readonly mmid = "navigationBar.sys.dweb") {
        super(mmid, "NavigationBar")
        this._appUrl = location.origin;
    }

    connectedCallback() {
        // 发起一个监听请求
        // this._addClickNavigatorbarItem()
    }


    /**
    * 显示导航栏。
    */
    async show(): Promise<void> {

    }

    /**
     * 隐藏导航栏。
     */
    async hide(): Promise<void> {

    }

    /**
     * 更改导航栏的颜色。
     *支持 alpha 十六进制数。
     * @param options 
     */
    async setColor(options: ColorParameters): Promise<void> {

    }

    /**
     * 设置透明度
     * @param isTransparent 
     */
    async setTransparency(options: { isTransparent: boolean }): Promise<void> {

    }

    /**
     * 以十六进制获取导航栏的当前颜色。
     */
    async getColor(): Promise<{ color: string }> {
        return { color: " " }
    }

    /**
     * 导航栏显示后触发的事件
     * @param event The event
     * @param listenerFunc Callback 
     */
    addListener_show(
        event: NavigationBarPluginEvents.SHOW,
        listenerFunc: () => void
    ): PluginListenerHandle {
        return this.addListener(event, listenerFunc)
    }

    /**
     * 导航栏隐藏后触发的事件
     * @param event The event
     * @param listenerFunc Callback 
     */
    addListener_hide(
        event: NavigationBarPluginEvents.HIDE,
        listenerFunc: () => void
    ): PluginListenerHandle {
        return this.addListener(event, listenerFunc)
    }

    /**
     * 导航栏颜色更改后触发的事件
     * @param event The event
     * @param listenerFunc Callback 
     */
    addListener_change(
        event: NavigationBarPluginEvents.COLOR_CHANGE,
        listenerFunc: (returnObject: { color: string }) => void
    ): PluginListenerHandle {
        return this.addListener(event, listenerFunc)
    }
}

customElements.define('navigatorbar-dweb', Navigatorbar);

// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
    const el = new Navigatorbar();
    document.body.append(el);
    document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
};
