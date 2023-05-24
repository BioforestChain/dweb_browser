"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.MultiWebviewCompStatusBar = void 0;
// 状态栏
const lit_1 = require("lit");
const decorators_js_1 = require("lit/decorators.js");
const style_map_js_1 = require("lit/directives/style-map.js");
const when_js_1 = require("lit/directives/when.js");
const electron_1 = require("electron");
const helper_js_1 = require("../../../plugins/helper.js");
let MultiWebviewCompStatusBar = class MultiWebviewCompStatusBar extends lit_1.LitElement {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "_color", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "#FFFFFFFF"
        });
        Object.defineProperty(this, "_style", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "DEFAULT"
        });
        Object.defineProperty(this, "_overlay", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "_visible", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: true
        });
        Object.defineProperty(this, "_height", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "38px"
        });
        Object.defineProperty(this, "_insets", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: {
                top: this._height,
                right: 0,
                bottom: 0,
                left: 0
            }
        });
        Object.defineProperty(this, "_torchIsOpen", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "_webview_src", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: {}
        });
    }
    createBackgroundStyleMap() {
        return {
            backgroundColor: this._visible
                ? this._overlay ? "transparent" : this._color
                : "#000000FF"
        };
    }
    createContainerStyleMap() {
        const isLight = globalThis.matchMedia('(prefers-color-scheme: light)');
        return {
            color: this._style === "LIGHT"
                ? "#000000FF"
                : this._style === "DARK"
                    ? "#FFFFFFFF"
                    : isLight
                        ? "#000000FF"
                        : "#FFFFFFFF"
        };
    }
    updated(_changedProperties) {
        const attributeProperties = Array.from(_changedProperties.keys());
        electron_1.ipcRenderer.send('status_bar_state_change', 
        // 数据格式 api.browser.sys.dweb-443.localhost:22605
        new URL(this._webview_src).host.replace('www.', "api."), {
            color: (0, helper_js_1.hexaToRGBA)(this._color),
            style: this._style,
            overlay: this._overlay,
            visible: this._visible,
            insets: this._insets
        });
        // 在影响 safe-area 的情况下 需要报消息发送给 safe-area 模块
        if (attributeProperties.includes('_visible')
            || attributeProperties.includes('_overlay')) {
            this.dispatchEvent(new Event("safe_area_need_update"));
        }
    }
    setHostStyle() {
        const host = this.renderRoot.host;
        host.style.position = this._overlay ? "absolute" : "relative";
        host.style.overflow = this._visible ? "visible" : "hidden";
    }
    render() {
        this.setHostStyle();
        const backgroundStyleMap = this.createBackgroundStyleMap();
        const containerStyleMap = this.createContainerStyleMap();
        return (0, lit_1.html) `
      <div class="comp-container">
        <div class="background" style=${(0, style_map_js_1.styleMap)(backgroundStyleMap)}></div>
        <div class="container" style=${(0, style_map_js_1.styleMap)(containerStyleMap)}>
          ${(0, when_js_1.when)(this._visible, () => (0, lit_1.html) `<div class="left_container">10:00</div>`, () => (0, lit_1.html) ``)}
          <div class="center_container">
            ${(0, when_js_1.when)(this._torchIsOpen, () => (0, lit_1.html) `<div class="torch_symbol"></div>`, () => (0, lit_1.html) ``)}
          </div>
          ${(0, when_js_1.when)(this._visible, () => (0, lit_1.html) `
                <div class="right_container">
                  <!-- 移动信号标志 -->
                  <svg 
                      t="1677291966287" 
                      class="icon icon-signal" 
                      viewBox="0 0 1024 1024" 
                      version="1.1" 
                      xmlns="http://www.w3.org/2000/svg" 
                      p-id="5745" 
                      width="32" 
                      height="32"
                  >
                      <path 
                          fill="currentColor"
                          d="M0 704h208v192H0zM272 512h208v384H272zM544 288h208v608H544zM816 128h208v768H816z" 
                          p-id="5746"
                      >
                      </path>
                  </svg>
      
                  <!-- wifi 信号标志 -->
                  <svg 
                      t="1677291873784" 
                      class="icon icon-wifi" 
                      viewBox="0 0 1024 1024" 
                      version="1.1" 
                      xmlns="http://www.w3.org/2000/svg" 
                      p-id="4699" 
                      width="32" 
                      height="32"
                  >
                      <path 
                          fill="currentColor"
                          d="M512 896 665.6 691.2C622.933333 659.2 569.6 640 512 640 454.4 640 401.066667 659.2 358.4 691.2L512 896M512 128C339.2 128 179.626667 185.173333 51.2 281.6L128 384C234.666667 303.786667 367.786667 256 512 256 656.213333 256 789.333333 303.786667 896 384L972.8 281.6C844.373333 185.173333 684.8 128 512 128M512 384C396.8 384 290.56 421.973333 204.8 486.4L281.6 588.8C345.6 540.586667 425.386667 512 512 512 598.613333 512 678.4 540.586667 742.4 588.8L819.2 486.4C733.44 421.973333 627.2 384 512 384Z" 
                          p-id="4700"
                      >
                      </path>
                  </svg>
      
                  <!-- 电池电量标志 -->
                  <svg 
                      t="1677291736404" 
                      class="icon icon-electricity" 
                      viewBox="0 0 1024 1024" 
                      version="1.1" 
                      xmlns="http://www.w3.org/2000/svg" 
                      p-id="2796" 
                      width="32" 
                      height="32"
                  >
                      <path 
                          fill="currentColor"
                          d="M984.2 434.8c-5-2.9-8.2-8.2-8.2-13.9v-99.3c0-53.6-43.9-97.5-97.5-97.5h-781C43.9 224 0 267.9 0 321.5v380.9C0 756.1 43.9 800 97.5 800h780.9c53.6 0 97.5-43.9 97.5-97.5v-99.3c0-5.8 3.2-11 8.2-13.9 23.8-13.9 39.8-39.7 39.8-69.2v-16c0.1-29.6-15.9-55.5-39.7-69.3zM912 702.5c0 12-6.2 19.9-9.9 23.6-3.7 3.7-11.7 9.9-23.6 9.9h-781c-11.9 0-19.9-6.2-23.6-9.9-3.7-3.7-9.9-11.7-9.9-23.6v-381c0-11.9 6.2-19.9 9.9-23.6 3.7-3.7 11.7-9.9 23.6-9.9h780.9c11.9 0 19.9 6.2 23.6 9.9 3.7 3.7 9.9 11.7 9.9 23.6v381z" 
                          fill="#606266" 
                          p-id="2797"
                      >
                      </path>
                      <path
                          fill="currentColor" 
                          d="M736 344v336c0 8.8-7.2 16-16 16H112c-8.8 0-16-7.2-16-16V344c0-8.8 7.2-16 16-16h608c8.8 0 16 7.2 16 16z" 
                          fill="#606266" 
                          p-id="2798"
                      >
                      </path>
                  </svg>
               </div>
              `, () => (0, lit_1.html) ``)}
        </div>
      </div>
    `;
    }
};
Object.defineProperty(MultiWebviewCompStatusBar, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: createAllCSS()
});
__decorate([
    (0, decorators_js_1.property)({ type: String })
], MultiWebviewCompStatusBar.prototype, "_color", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: String })
], MultiWebviewCompStatusBar.prototype, "_style", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Boolean })
], MultiWebviewCompStatusBar.prototype, "_overlay", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Boolean })
], MultiWebviewCompStatusBar.prototype, "_visible", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: String })
], MultiWebviewCompStatusBar.prototype, "_height", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Object })
], MultiWebviewCompStatusBar.prototype, "_insets", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Boolean })
], MultiWebviewCompStatusBar.prototype, "_torchIsOpen", void 0);
__decorate([
    (0, decorators_js_1.property)()
], MultiWebviewCompStatusBar.prototype, "_webview_src", void 0);
MultiWebviewCompStatusBar = __decorate([
    (0, decorators_js_1.customElement)('multi-webview-comp-status-bar')
], MultiWebviewCompStatusBar);
exports.MultiWebviewCompStatusBar = MultiWebviewCompStatusBar;
function createAllCSS() {
    return [
        (0, lit_1.css) `
      :host{
        z-index: 999999999;
        flex-grow: 0;
        flex-shrink: 0;
        width: 100%;
        height: 38px;
      }

      .comp-container{
        --height: 48px;
        --cell-width: 80px;
        position: relative;
        width: 100%;
        height: 100%;
      }

      // html{
      //   width:100%;
      //   height: var(--height);
      //   overflow: hidden;
      // }

      .background{
        position: absolute;
        left: 0px;
        top: 0px;
        width: 100%;
        height: 100%;
        background: #FFFFFFFF;
      }

      .container{
        position: absolute;
        left: 0px;
        top: 0px;
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height:100%;
      }

      .container-light{
        color: #FFFFFFFF;
      }

      .container-dark{
        color: #000000FF;
      }

      .container-default{
        color: #FFFFFFFF;
      }

      .left_container{
        display: flex;
        justify-content: center;
        align-items: flex-end;
        width: var(--cell-width);
        height: 100%;
        font-size: 15px;
        font-weight: 900;
      }

      .center_container{
        position: relative;
        display: flex;
        justify-content: center;
        align-items: center;
        width: calc(100% - var(--cell-width) * 2);
        height: 100%;
        border-bottom-left-radius: var(--border-radius);
        border-bottom-right-radius: var(--border-radius);
      }

      .center_container::after{
        content: "";
        width: 50%;
        height: 20px;
        border-radius: 10px;
        background: #111111;
      }

      .torch_symbol{
        position: absolute;
        z-index: 1;
        width: 10px;
        height: 10px;
        border-radius: 20px;
        background: #fa541c;
      }

      .right_container{
        display: flex;
        justify-content: flex-start;
        align-items: flex-end;
        width: var(--cell-width);
        height: 100%;
      }

      .icon{
        margin-right: 5px;
        width: 18px;
        height: 18px;
      }
    `
    ];
}
