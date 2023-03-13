import { BasePlugin } from '../basePlugin.ts';
import { NavigationBarPluginEvents } from "./navigator.events.ts";
import { ColorParameters } from "./navigator.type.ts"
// navigator-bar 插件 
export class Navigatorbar extends BasePlugin {

    constructor(readonly mmid = "navigationbar.sys.dweb") {
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
        await this.nativeFetch("/setVisible", {
            search: {
                visible: true
            }
        })
    }

    /**
     * 隐藏导航栏。
     */
    async hide(): Promise<void> {
        await this.nativeFetch("/setVisible", {
            search: {
                visible: false
            }
        })
    }


    /**
     * 获取导航栏是否可见。
     */
    async getVisible(): Promise<Response> {
        return await this.nativeFetch("/getVisible")
    }

    /**
     * 更改导航栏的颜色。
     *支持 alpha 十六进制数。
     * @param options 
     */
    async setColor(options: ColorParameters): Promise<void> {
        await this.nativeFetch("/setBackgroundColor", {
            search: {
                color: options.color,
                darkButtons: options.darkButtons
            }
        })
    }


    /**
     * 以十六进制获取导航栏的当前颜色。
     */
    async getColor(): Promise<{ color: string }> {
        const color = await this.nativeFetch("/getBackgroundColor").then(res => res.text())
        return { color: color }
    }

    /**
     * 设置透明度
     * @param isTransparent 
     */
    async setTransparency(options: { isTransparent: boolean }): Promise<void> {
        await this.nativeFetch("/setTransparency", {
            search: {
                isTransparency: options.isTransparent,
            }
        })
    }
    /**
     * 获取导航栏是否透明度
     * @param isTransparent 
     */
    async getTransparency(): Promise<Response> {
        return await this.nativeFetch("/getTransparency")
    }

    /**
     * 设置导航栏是否覆盖内容
     * @param isOverlay 
     */
    async setOverlay(options: { isOverlay: boolean }): Promise<void> {
        await this.nativeFetch("/setOverlay", {
            search: {
                isOverlay: options.isOverlay,
            }
        })
    }
    /**
     * 获取导航栏是否覆盖内容
     * @param isOverlay 
     */
    async getOverlay(): Promise<Response> {
        return await this.nativeFetch("/getOverlay")
    }

    _signalShow = this.createSignal<ListenerCallback>()
    _signalHide = this.createSignal<ListenerCallback>()
    _signalChange = this.createSignal<ListenerCallback>()


    /**
     * 导航栏的事件
     * @param event The event
     * @param listenerFunc Callback 
     * NavigationBarPluginEvents.SHOW 导航栏显示后触发的事件
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
