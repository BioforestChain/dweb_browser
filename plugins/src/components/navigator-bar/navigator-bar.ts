import { BasePlugin } from '../basePlugin.ts';
import { NavigationBarPluginEvents } from "./navigator.events.ts";
import { ColorParameters } from "./navigator.type.ts"
// navigator-bar 插件 
export class Navigatorbar extends BasePlugin {

    constructor(readonly mmid = "navigationBar.sys.dweb") {
        super(mmid, "NavigationBar")
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

    private readonly _signalShow = this.createSignal<ListenerCallback>()
    private readonly _signalHide = this.createSignal<ListenerCallback>()
    private readonly _signalChange = this.createSignal<ListenerCallback>()


    /**
     * 导航栏显示后触发的事件
     * @param event The event
     * @param listenerFunc Callback 
     * NavigationBarPluginEvents.HIDE 导航栏隐藏后触发的事件
     * NavigationBarPluginEvents.COLOR_CHANGE 导航栏颜色更改后触发的事件
     */
    addListener(
        event: NavigationBarPluginEvents,
        listenerFunc: ListenerCallback
    ) {
        switch (event) {
            case NavigationBarPluginEvents.HIDE:
                return this._signalHide.listen(listenerFunc)
            case NavigationBarPluginEvents.COLOR_CHANGE:
                return this._signalChange.listen(listenerFunc)
            default: return this._signalShow.listen(listenerFunc)
        }
    }

}

// deno-lint-ignore no-explicit-any
export type ListenerCallback = (...args: any[]) => void;
