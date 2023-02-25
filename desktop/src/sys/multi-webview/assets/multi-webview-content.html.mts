// 效果 webview 容器
import { css , html, LitElement } from "lit"
import { customElement, property, state } from "lit/decorators.js"
import { styleMap } from 'lit/directives/style-map.js';
import { ifDefined } from "lit/directives/if-defined.js"; 
import { Webview } from "./multi-webview.mjs";
import WebviewTag = Electron.WebviewTag

const allCss = [
    css`
        :host{
            --status-bar-height: 47px;
            --navigation-bar-height: 64px;
            --border-radius: 46px;
            --cmera-container-zindex: 999;
            --bottom-line-container-height:20px;
            width:100%;
            height:100%;
        }
        
        .container {
            position: relative;
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            align-items: center;
            box-sizing: border-box;
            width: 100%;
            height: 100%;
            border: 10px solid #333;
            border-radius:var(--border-radius);
            overflow: hidden;
        }

        .iframe-statusbar{
            
            // box-sizing: border-box;
            // width:100%;
            // height: 48px;
        }

        .bottom-line-container{
            position: absolute;
            z-index: 1;
            left:0px;
            bottom:0px;
            display: flex;
            justify-content: center;
            align-items: flex-start;
            width:100%;
            height: var(--bottom-line-container-height);
        }

        .bottom-line-container::after{
            content: "";
            width: 50%;
            height:4px;
            border-radius:4px;
            background: #000;
        }

        .webview-container{
            flex-grow: 100;
            flex-shrink: 100;
            box-sizing: border-box;
            width:100%;
            height:100%;
            scrollbar-width: 2px;
            overflow: hidden;
            overflow-y: auto;
            /* border: 1px solid red; */
        }
        .webview{
            box-sizing: border-box;
            width:100%;
            min-height:100%;
            height: auto;
            
        }

    `,
    // 需要啊全部的custom.属性传递进来
    // 动画相关
    css`
        :host {
            --easing: cubic-bezier(0.36, 0.66, 0.04, 1);
        }
        .opening-ani-view {
            animation: slideIn 520ms var(--easing) forwards;
        }
        .closing-ani-view {
            animation: slideOut 830ms var(--easing) forwards;
        }
        @keyframes slideIn {
            0% {
                transform: translateY(60%) translateZ(0);
                opacity: 0.4;
            }
            100% {
                transform: translateY(0%) translateZ(0);
                opacity: 1;
            }
        }
        @keyframes slideOut {
            0% {
                transform: translateY(0%) translateZ(0);
                opacity: 1;
            }
            30% {
                transform: translateY(-30%) translateZ(0) scale(0.4);
                opacity: 0.6;
            }
            100% {
                transform: translateY(-100%) translateZ(0) scale(0.3);
                opacity: 0.5;
            }
        }
    `,
]



@customElement("multi-webview-content")
export class MultiWebViewContent extends LitElement{
    @property({type: Webview}) customWebview: Webview | undefined = undefined;
    @property({type: Boolean}) closing: Boolean = false;
    @property({type: Number}) zIndex: Number = 0;
    @property({type: Number}) scale: Number = 0;
    @property({type: Number}) opacity: Number = 1;
    @property({type: Number}) customWebviewId: Number = 0;
    @property({type: String}) src: String = ""
    @state() statusbarHidden: boolean = false;

    static override styles  = allCss

    onDomReady(event: Event){
        this.dispatchEvent(new CustomEvent(
            "dom-ready",
            {
                bubbles: true,
                detail: {
                    customWebview: this.customWebview,
                    event: event,
                    from: event.target
                }
            }
        ))
    }

    onAnimationend(event: AnimationEvent){
        this.dispatchEvent(new CustomEvent(
            "animationend",
            {
                bubbles: true,
                detail: {
                    customWebview: this.customWebview,
                    event: event,
                    from: event.target
                }
            }
        ))
    }

    override render(){
        const containerStyleMap = styleMap({
            "--z-index": this.zIndex + "",
            "--scale": this.scale + "",
            "--opacity": this.opacity + ""
        })

        const host = document.querySelector(":host")
        // 如何把状态栏 这个 自定义标签 <dweb-status-bar /> 这个 自定义标签注入进去
        // 同时 如何把这个额dweb-status-bar 这个标签同 statusbar.sys.dweb NMM 模块关联起来；
        // 是否可以通过多为 <webview> 标签实现 通过 jsMM 实现？？
        // 
        return html`
            <div 
                class="container ${ this.closing ? `closing-ani-view` : `opening-ani-view`}"
                style="${containerStyleMap}"  
                @animationend=${this.onAnimationend}  
            >
                <!-- 启动了服务后确实能够显示内容 webview 请求状态栏的服务-->
                <iframe
                    id="statusbar"
                    class="iframe-statusbar"
                    style="width: 100%;height: 48px;border: none; flex-grow: 0; flex-shrink: 0; position: relative; left: 0px; top: 0px;"
                    src="http://statusbar.sys.dweb-80.localhost:22605/"
                    @load=${() => console.log('statusbar 载入完成')}
                ></iframe>
                <!-- 底部黑线 -->
                <div class="bottom-line-container"></div>
                <!-- 内容容器 -->
                <div class="webview-container">
                    <!-- 这个 webview 是如何载入的了？？？ -->
                    <webview
                        id="view-${this.customWebviewId}"
                        class="webview"
                        src=${ifDefined(this.src)}
                        partition="trusted"
                        allownw
                        allowpopups
                        @dom-ready=${this.onDomReady}
                    ></webview>
                </div>
            </div>
        `
    }
}

// 导出类型

export interface CustomEventDomReadyDetail{
    customWebview: Webview ;
    event: Event,
    from: EventTarget & WebviewTag;
}

export interface CustomEventAnimationendDetail{
    customWebview: Webview,
    event: AnimationEvent,
    from: EventTarget | null
}
