var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
// 效果 webview 容器
import { css, html, LitElement } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import { styleMap } from "lit/directives/style-map.js";
import { ifDefined } from "lit/directives/if-defined.js";
import { Webview } from "./multi-webview.js";
import ecxuteJavascriptCode from "./multi-webview-content-execute-javascript.js";
let MultiWebViewContent = class MultiWebViewContent extends LitElement {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "customWebview", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: undefined
        });
        Object.defineProperty(this, "closing", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "zIndex", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "scale", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "opacity", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 1
        });
        Object.defineProperty(this, "customWebviewId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "src", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ""
        });
        Object.defineProperty(this, "preload", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ""
        });
        Object.defineProperty(this, "statusbarHidden", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "elWebview", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         * 向内部的 webview 的内容执行 code
         * @param code
         */
        Object.defineProperty(this, "executeJavascript", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (code) => {
                if (this.elWebview === undefined)
                    throw new Error(`this.elWebview === undefined`);
                this.elWebview.executeJavaScript(code);
            }
        });
    }
    onDomReady(event) {
        this.dispatchEvent(new CustomEvent("dom-ready", {
            bubbles: true,
            detail: {
                customWebview: this.customWebview,
                event: event,
                from: event.target,
            },
        }));
        console.log("onDomReady");
    }
    webviewDidStartLoading(e) {
        console.log("did-start-loading");
        const el = e.target;
        if (el === null)
            throw new Error(`el === null`);
        e.target.executeJavaScript(ecxuteJavascriptCode);
    }
    onAnimationend(event) {
        this.dispatchEvent(new CustomEvent("animationend", {
            bubbles: true,
            detail: {
                customWebview: this.customWebview,
                event: event,
                from: event.target,
            },
        }));
    }
    onPluginNativeUiLoadBase(e) {
        const iframe = e.target;
        const contentWindow = iframe.contentWindow;
        // 把 status-bar 添加到容器上
        // 把容器 元素传递给内部的内部的window
        const container = iframe.parentElement;
        Reflect.set(contentWindow, "parentElement", container);
        contentWindow.postMessage("loaded", "*");
    }
    getWebviewTag() {
        return this.elWebview;
    }
    render() {
        const containerStyleMap = styleMap({
            "--z-index": this.zIndex + "",
            "--scale": this.scale + "",
            "--opacity": this.opacity + "",
        });
        // useragent=${navigator.userAgent + "dweb-host/" + location.host}
        return html `
      <webview
        nodeintegration
        nodeintegrationinsubframes
        allowpopups
        disablewebsecurity
        plugins
        class="webview ${this.closing
            ? `closing-ani-view`
            : `opening-ani-view`}"
        style="${containerStyleMap}"
        @animationend=${this.onAnimationend}
        data-app-url=${this.src}
        id="view-${this.customWebviewId}"
        class="webview"
        src=${ifDefined(this.src)}
        partition="trusted"
        allownw
        preload=${this.preload}
        @dom-ready=${this.onDomReady}
        @did-start-loading=${this.webviewDidStartLoading}
        useragent=${navigator.userAgent + " dweb-host/" + location.host}
      ></webview>
    `;
    }
};
Object.defineProperty(MultiWebViewContent, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: createAllCSS()
});
__decorate([
    property({ type: Webview })
], MultiWebViewContent.prototype, "customWebview", void 0);
__decorate([
    property({ type: Boolean })
], MultiWebViewContent.prototype, "closing", void 0);
__decorate([
    property({ type: Number })
], MultiWebViewContent.prototype, "zIndex", void 0);
__decorate([
    property({ type: Number })
], MultiWebViewContent.prototype, "scale", void 0);
__decorate([
    property({ type: Number })
], MultiWebViewContent.prototype, "opacity", void 0);
__decorate([
    property({ type: Number })
], MultiWebViewContent.prototype, "customWebviewId", void 0);
__decorate([
    property({ type: String })
], MultiWebViewContent.prototype, "src", void 0);
__decorate([
    property({ type: String })
], MultiWebViewContent.prototype, "preload", void 0);
__decorate([
    state()
], MultiWebViewContent.prototype, "statusbarHidden", void 0);
__decorate([
    query("webview")
], MultiWebViewContent.prototype, "elWebview", void 0);
MultiWebViewContent = __decorate([
    customElement("multi-webview-content")
], MultiWebViewContent);
export { MultiWebViewContent };
function createAllCSS() {
    return [
        css `
      :host {
        box-sizing: border-box;
        margin: 0px;
        padding: 0px;
        width: 100%;
        height: 100%;
      }

      .webview {
        position: relative;
        box-sizing: border-box;
        width: 100%;
        min-height: 100%;
        scrollbar-width: 2px;
        overflow: hidden;
        overflow-y: auto;
      }
    `,
        // 需要啊全部的custom.属性传递进来
        // 动画相关
        css `
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
    ];
}
