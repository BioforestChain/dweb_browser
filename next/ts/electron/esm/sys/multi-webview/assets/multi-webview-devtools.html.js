var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement } from "lit/decorators.js";
import { property } from "lit/decorators.js";
import { styleMap } from "lit/directives/style-map.js";
import { Webview } from "./multi-webview.js";
const allCss = [
    css `
    :host{
      width:100%;
      height:100%;
    }

    .container{
      width:100%;
      height:100%;
    }

    .toolbar{
      display: flex;
      justify-content: flex-start;
      align-items: flex-start;
      width:100%;
      height:60px;
    }

    .devtool{
      width:100%;
      height:calc(100% - 60px);
      border:1px solid #ddd;
    }
  `,
    // 需要啊全部的custom.属性传递进来
    // 动画相关
    css `
    :host {
      --easing: cubic-bezier(0.36, 0.66, 0.04, 1);
    }
    .opening-ani-devtools {
      animation: slideIn 520ms var(--easing) forwards;
    }
    .closing-ani-devtools {
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
let MultiWebviewDevtools = class MultiWebviewDevtools extends LitElement {
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
    }
    onDomReady(event) {
        this.dispatchEvent(new CustomEvent("dom-ready", {
            bubbles: true,
            detail: {
                customWebview: this.customWebview,
                event: event,
                from: event.target
            }
        }));
    }
    onDestroy() {
        this.dispatchEvent(new Event("destroy-webview"));
    }
    render() {
        const containerStyleMap = styleMap({
            "--z-index": this.zIndex + "",
            "--scale": this.scale + "",
            "--opacity": this.opacity + ""
        });
        return html `
      <div 
        class="container ${this.closing ? 'closing-ani-devtools' : 'opening-ani-devtools'}" 
        style=${containerStyleMap}
      >
        <div class="toolbar">
            <button @click=${this.onDestroy}>销毁</button>
        </div>
        <webview
          id="tool-${this.customWebviewId}"
          class="devtool"
          src="about:blank"
          partition="trusted"
          @dom-ready=${this.onDomReady}
        ></webview>
      </div>
    `;
    }
};
Object.defineProperty(MultiWebviewDevtools, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: allCss
});
__decorate([
    property({ type: Webview })
], MultiWebviewDevtools.prototype, "customWebview", void 0);
__decorate([
    property({ type: Boolean })
], MultiWebviewDevtools.prototype, "closing", void 0);
__decorate([
    property({ type: Number })
], MultiWebviewDevtools.prototype, "zIndex", void 0);
__decorate([
    property({ type: Number })
], MultiWebviewDevtools.prototype, "scale", void 0);
__decorate([
    property({ type: Number })
], MultiWebviewDevtools.prototype, "opacity", void 0);
__decorate([
    property({ type: Number })
], MultiWebviewDevtools.prototype, "customWebviewId", void 0);
MultiWebviewDevtools = __decorate([
    customElement("multi-webview-devtools")
], MultiWebviewDevtools);
export { MultiWebviewDevtools };
