var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
/**
 * app 容器组件
 * - status-bar
 * - ...其他插件都在这里注入
 * - 用来模拟移动端硬件外壳
 * - 仅仅只有 UI 不提供非 UI 之外的任何交互功能
 */
import { css, html, LitElement } from "lit";
import { customElement } from "lit/decorators.js";
import { query } from "lit/decorators.js";
let MultiWebViewCompMobileShell = class MultiWebViewCompMobileShell extends LitElement {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "appContentContainer", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
    }
    /**
     *
     * @param message
     * @param duration
     * @param position
     */
    toastShow(message, duration, position) {
        const multiWebviewCompToast = document.createElement('multi-webview-comp-toast');
        [
            ["_message", message],
            ["_duration", duration],
            ["_position", position],
        ].forEach(([key, value]) => {
            multiWebviewCompToast.setAttribute(key, value);
        });
        this.appContentContainer?.append(multiWebviewCompToast);
    }
    setHostStyle() {
        const host = this.renderRoot.host;
    }
    biometricsMock() {
        const el = document.createElement('multi-webview-comp-biometrics');
        el.addEventListener('pass', () => {
            this.dispatchEvent(new Event("biometrices-pass"));
        });
        el.addEventListener('no-pass', () => {
            this.dispatchEvent(new Event('biometrices-no-pass'));
        });
        this.appContentContainer?.appendChild(el);
        console.log('biometrics', el, this.appContentContainer);
    }
    hapticsMock(text) {
        console.log("hapticsMock", text);
        const el = document.createElement('multi-webview-comp-haptics');
        el.setAttribute('text', text);
        this.appContentContainer?.appendChild(el);
    }
    shareShare(options) {
        const el = document.createElement('multi-webview-comp-share');
        [
            ["_title", options.title],
            ["_text", options.text],
            ["_link", options.link],
            ["_src", options.src]
        ].forEach(([key, value]) => el.setAttribute(key, value));
        this.appContentContainer?.appendChild(el);
    }
    render() {
        this.setHostStyle();
        return html `
      <div class="shell_container">
        <slot name="status-bar"></slot>
        <div class="app_content_container">
          <slot name="app_content">
            ... 桌面 ...
          </slot>
        </div>
        <slot name="bottom-bar"></slot>
      </div>
    `;
    }
};
Object.defineProperty(MultiWebViewCompMobileShell, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: createAllCSS()
});
__decorate([
    query('.app_content_container')
], MultiWebViewCompMobileShell.prototype, "appContentContainer", void 0);
MultiWebViewCompMobileShell = __decorate([
    customElement("multi-webview-comp-mobile-shell")
], MultiWebViewCompMobileShell);
export { MultiWebViewCompMobileShell };
function createAllCSS() {
    return [
        css `
      :host{
        overflow: hidden;
      }

      .shell_container{
        --width: 375px;
        position: relative;
        display: flex;
        flex-direction: column;
        box-sizing: content-box;
        width: var(--width);
        height: calc(var(--width) * 2.05);
        border: 10px solid #000000;
        border-radius: calc(var(--width) / 12);
        overflow: hidden;
      }

      .app_content_container{
        position: relative;
        box-sizing: border-box;
        width: 100%;
        height: 100%;
      }
    `
    ];
}
