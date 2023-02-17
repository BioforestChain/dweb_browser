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
            box-sizing: border-box;
            width: 100%;
            height: 100%;
            border: 10px solid #333;
            border-radius:var(--border-radius);
            overflow: hidden;
            border-radius:var(--border-radius);
        }

        .camera-container{
            position: absolute;
            z-index: var(--cmera-container-zindex);
            left: 0px;
            top: 0px;
            display: flex;
            justify-content: center;
            width:100%;
            height:var(--status-bar-height);
            background-color: transparent;
        }

        .camera-container::after{
            --border-radius:10px;
            content: "";
            width: 56%;
            height:100%;
            border-bottom-left-radius: var(--border-radius);
            border-bottom-right-radius: var(--border-radius);
            background:#000;
        }

        .status-navigation-bar-container{
            position: absolute;
            z-index: 1;
            left: 0px;
            top: 0px;
            width:100%;
            height:auto;
            /* border-top-left-radius: var(--border-radius);
            border-top-right-radius:var(--border-radius); */
            
            overflow: hidden;
        }

        .status-bar-contaienr{
            width:100%;
            height: var(--status-bar-height);
            background-color:#0002;
        }

        .navigation-bar-container{
            width:100%;
            height:var(--navigation-bar-height);
            background-color: #0001;
        }

        .status-navigation-bar-container-statusbar-hidden{
            background-color: transparent;
        }

        .status-navigation-bar-container-statusbar-show{
            background-color: #0002;
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
            width:100%;
            height:100%;
            /* border-radius:var(--border-radius); */
            overflow: hidden;
        }

        .webview-container-statusbar-hidden{
            padding-top:0px;
        }

        .webview-container-statusbar-show{
            padding-top:calc(var(--status-bar-height) + var(--navigation-bar-height));
        }

        .webview{
            width:100%;
            height:100%;
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

        return html`
            <div 
                class="container ${ this.closing ? `closing-ani-view` : `opening-ani-view`}"
                style="${containerStyleMap}"  
                @animationend=${this.onAnimationend}  
            >
                <div class="camera-container"></div>
                <!-- 状态栏 + 导航栏 -->
                <div 
                    class="
                        status-navigation-bar-container
                        ${this.statusbarHidden ? "status-navigation-bar-container-statusbar-hidden" : "status-navigation-bar-container-statusbar-show"}
                    "
                >
                    <div class="status-bar-contaienr"></div>
                    <div class="navigation-bar-container"></div>
                </div>
                <!-- 底部黑线 -->
                <div class="bottom-line-container"></div>
                <!-- 内容容器 -->
                <div 
                    class="
                        webview-container 
                        ${this.statusbarHidden ? "webview-container-statusbar-hidden" : "webview-container-statusbar-show"}
                    ">
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
