"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.APIS = exports.ViewTree = void 0;
/// <reference lib="DOM"/>
require("./multi-webview-content.html.js");
require("./multi-webview-devtools.html.js");
require("./components/multi-webview-comp-status-bar.html.js");
require("./components/multi-webview-comp-mobile-shell.html.js");
require("./components/multi-webview-comp-navigator-bar.html.js");
require("./components/multi-webview-comp-virtual-keyboard.html.js");
require("./components/multi-webview-comp-toast.html.js");
require("./components/multi-webview-comp-barcode-scanning.html.js");
require("./components/multi-webview-comp-biometrics.html.js");
require("./components/multi-webview-comp-haptics.html.js");
require("./components/multi-webview-comp-share.html.js");
const multi_webview_excute_javascript_js_1 = __importDefault(require("./multi-webview-excute-javascript.js"));
const lit_1 = require("lit");
const repeat_js_1 = require("lit/directives/repeat.js");
const style_map_js_1 = require("lit/directives/style-map.js");
const when_js_1 = require("lit/directives/when.js");
const decorators_js_1 = require("lit/decorators.js");
const comlink_1 = require("comlink");
const openNativeWindow_preload_js_1 = require("../../../helper/openNativeWindow.preload.js");
const multi_webview_comp_safe_area_shim_js_1 = require("./components/multi-webview-comp-safe-area.shim.js");
const multi_webview_js_1 = require("./multi-webview.js");
const electron_1 = require("electron");
let ViewTree = class ViewTree extends lit_1.LitElement {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "_id_acc", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "webviews", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: []
        });
        Object.defineProperty(this, "statusBarHeight", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "38px"
        });
        Object.defineProperty(this, "navigationBarHeight", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "26px"
        });
        Object.defineProperty(this, "virtualKeyboardAnimationSetIntervalId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "_multiWebviewContent", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "multiWebviewCompMobileShell", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "multiWebviewCompVirtualKeyboard", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "multiWebviewCompNavigationBar", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "name", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "Multi Webview"
        });
        Object.defineProperty(this, "statusBarState", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: []
        });
        Object.defineProperty(this, "navigationBarState", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: []
        });
        Object.defineProperty(this, "safeAreaState", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: []
        });
        Object.defineProperty(this, "isShowVirtualKeyboard", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "virtualKeyboardState", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: {
                insets: {
                    top: 0,
                    right: 0,
                    bottom: 0,
                    left: 0
                },
                overlay: false,
                visible: false
            }
        });
        Object.defineProperty(this, "torchState", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: { isOpen: false }
        });
        Object.defineProperty(this, "preloadAbsolutePath", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ""
        });
        Object.defineProperty(this, "safeAreaGetState", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: () => {
                const navigationBarState = this.navigationBarState[0];
                const statusbarState = this.statusBarState[0];
                const bottomBarState = (0, multi_webview_comp_safe_area_shim_js_1.getButtomBarState)(navigationBarState, this.isShowVirtualKeyboard, this.virtualKeyboardState);
                return {
                    overlay: this.safeAreaState[0].overlay,
                    insets: {
                        left: 0,
                        top: statusbarState.visible
                            ? statusbarState.overlay ? statusbarState.insets.top : 0
                            : statusbarState.insets.top,
                        right: 0,
                        bottom: bottomBarState.visible
                            ? bottomBarState.overlay ? bottomBarState.insets.bottom : 0
                            : 0
                    },
                    cutoutInsets: {
                        left: 0,
                        top: statusbarState.insets.top,
                        right: 0,
                        bottom: 0,
                    },
                    // 外部尺寸
                    outerInsets: {
                        left: 0,
                        top: statusbarState.visible
                            ? statusbarState.overlay ? 0 : statusbarState.insets.top
                            : 0,
                        right: 0,
                        bottom: bottomBarState.visible
                            ? bottomBarState.overlay ? 0 : bottomBarState.insets.bottom
                            : 0
                    }
                };
            }
        });
        Object.defineProperty(this, "safeAreaSetOverlay", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (overlay) => {
                const state = this.safeAreaState;
                state[0].overlay = overlay;
                this.safeAreaState = state;
                this.barSetState("statusBarState", "overlay", overlay);
                this.barSetState("navigationBarState", "overlay", overlay);
                this.virtualKeyboardSetOverlay(overlay);
                return this.safeAreaGetState();
            }
        });
        Object.defineProperty(this, "safeAreaNeedUpdate", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: () => {
                electron_1.ipcRenderer.send("safe_area_update", new URL(this.webviews[this.webviews.length - 1].src).host.replace("www.", "api."), this.safeAreaGetState());
            }
        });
        Object.defineProperty(this, "biometricesResolve", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         * navigation-bar 点击 back 的事件处理器
         * 业务逻辑：
         * 向 webview 添加一个 ipc-message 事件监听器
         * 向 webview 注入执行一段 javascript code
         * @returns
         */
        Object.defineProperty(this, "navigationBarOnBack", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: () => {
                const len = this.webviews.length;
                const webview = this.webviews[0];
                const origin = new URL(webview.src).origin;
                this._multiWebviewContent?.forEach(el => {
                    if (el.src.includes(origin)) {
                        const webview = el.getWebviewTag();
                        webview?.addEventListener('ipc-message', this.webviewTagOnIpcMessageHandlerBack);
                    }
                });
                // executre 通过 fetch 把消息发送出来
                this.executeJavascriptByHost(origin, multi_webview_excute_javascript_js_1.default);
            }
        });
        Object.defineProperty(this, "webviewTagOnIpcMessageHandlerBack", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (e) => {
                const channel = Reflect.get(e, "channel");
                const args = Reflect.get(e, 'args');
                if (channel === "webveiw_message" && args[0] === "back" && e.target !== null) {
                    e.target.removeEventListener('ipc-message', this.webviewTagOnIpcMessageHandlerBack);
                    // 需要从 webviews 删除第一位
                    this.navigationBarState = this.navigationBarState.slice(1);
                    this.statusBarState = this.statusBarState.slice(1);
                    // 把 navigationBarState statusBarStte safe-area 的改变发出
                    electron_1.ipcRenderer.send('safe_are_insets_change');
                    electron_1.ipcRenderer.send('navigation_bar_state_change');
                    electron_1.ipcRenderer.send('status_bar_state_change');
                }
                const len = this.webviews.length;
                len === 1
                    ? openNativeWindow_preload_js_1.mainApis.closedBrowserWindow()
                    : this.destroyWebview(this.webviews[0]);
            }
        });
        Object.defineProperty(this, "webviewTagOnIpcMessageHandlerNormal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (e) => {
                const channel = Reflect.get(e, "channel");
                const args = Reflect.get(e, 'args');
                switch (channel) {
                    case "virtual_keyboard_open":
                        this.isShowVirtualKeyboard = true;
                        break;
                    case "virtual_keyboard_close":
                        this.virtualKeyboardState = {
                            ...this.virtualKeyboardState,
                            visible: false
                        };
                        break;
                    case "webveiw_message":
                        this.webviewTagOnIpcMessageHandlerBack(e);
                        break;
                    default: throw new Error(`webview ipc-message 还有没有处理的channel===${channel}`);
                }
            }
        });
    }
    barSetState(propertyName, key, value) {
        const state = this[propertyName];
        const len = state.length;
        state[0][key] = value;
        // 如果改变的 navigationBarState.visible 
        // 还需要改变 insets.bottom 的值
        if (propertyName === "navigationBarState" && key === "visible") {
            state[0].insets.bottom = value ? parseInt(this.navigationBarHeight) : 0;
        }
        this[propertyName] = JSON.parse(JSON.stringify(state));
        return this[propertyName][0];
    }
    barGetState(propertyName) {
        const i = this[propertyName].length - 1;
        return this[propertyName][i];
    }
    /**
     * 根据 multi-webview-comp-virtual-keyboard 标签
     * 和 navigationBarState.insets.bottom 的值
     * 设置 virtualKeyboardState.inserts.bottom
     */
    virtualKeyboardStateUpdateInsetsByEl() {
        const height = this.multiWebviewCompVirtualKeyboard?.getBoundingClientRect().height;
        if (height === undefined)
            throw new Error(`height === undefined`);
        const currentNavigationBarHeight = this.navigationBarState[this.navigationBarState.length - 1].insets.bottom;
        this.virtualKeyboardState = {
            ...this.virtualKeyboardState,
            insets: {
                ...this.virtualKeyboardState.insets,
                bottom: height <= currentNavigationBarHeight ? 0 : height
            }
        };
        // 需要把改变发送给 sare-area
        this.safeAreaNeedUpdate();
    }
    virtualKeyboardFirstUpdated() {
        this.virtualKeyboardState = {
            ...this.virtualKeyboardState,
            visible: true
        };
    }
    virtualKeyboardHideCompleted() {
        this.isShowVirtualKeyboard = false;
        electron_1.ipcRenderer.send('safe_are_insets_change');
        clearInterval(this.virtualKeyboardAnimationSetIntervalId);
    }
    virtualKeyboardShowCompleted() {
        electron_1.ipcRenderer.send('safe_are_insets_change');
        clearInterval(this.virtualKeyboardAnimationSetIntervalId);
    }
    virtualKeyboardSetOverlay(overlay) {
        const state = {
            ...this.virtualKeyboardState,
            overlay: overlay
        };
        this.virtualKeyboardState = state;
        return state;
    }
    virtualKeyboardGetState() {
        return this.virtualKeyboardState;
    }
    toastShow(message, duration, position) {
        this.multiWebviewCompMobileShell?.toastShow(message, duration, position);
    }
    torchStateToggle() {
        const state = {
            ...this.torchState,
            isOpen: !this.torchState.isOpen
        };
        this.torchState = state;
        return state.isOpen;
    }
    torchStateGet() {
        return this.torchState.isOpen;
    }
    barcodeScanningGetPhoto() {
        const el = document.createElement('multi-webview-comp-barcode-scanning');
        document.body.append(el);
    }
    biometricsMock() {
        this.multiWebviewCompMobileShell?.biometricsMock();
        return new Promise(resolve => this.biometricesResolve = resolve);
    }
    biometricessPass(b) {
        this.biometricesResolve?.(b);
    }
    hapticsSet(value) {
        this.multiWebviewCompMobileShell?.hapticsMock(value);
        return true;
    }
    shareShare(options) {
        this.multiWebviewCompMobileShell?.shareShare(options);
    }
    /** 对webview视图进行状态整理 */
    _restateWebviews() {
        let index_acc = 0;
        let closing_acc = 0;
        let opening_acc = 0;
        let scale_sub = 0.05;
        let scale_acc = 1 + scale_sub;
        let opacity_sub = 0.1;
        let opacity_acc = 1 + opacity_sub;
        for (const webview of this.webviews) {
            webview.state.zIndex = this.webviews.length - ++index_acc;
            if (webview.closing) {
                webview.state.closingIndex = closing_acc++;
            }
            else {
                {
                    webview.state.scale = scale_acc -= scale_sub;
                    scale_sub = Math.max(0, scale_sub - 0.01);
                }
                {
                    webview.state.opacity = opacity_acc - opacity_sub;
                    opacity_acc = Math.max(0, opacity_acc - opacity_sub);
                }
                {
                    webview.state.openingIndex = opening_acc++;
                }
            }
        }
        this.requestUpdate("webviews");
    }
    // 打开一个新的webview标签
    // 在html中执行 open() 也会调用这个方法
    openWebview(src) {
        const webview_id = this._id_acc++;
        // 都从最前面插入
        this.webviews.unshift(new multi_webview_js_1.Webview(webview_id, src));
        this._restateWebviews();
        if (this.webviews.length === 1) {
            this.statusBarState = [createDefaultBarState('statusbar', this.statusBarHeight)];
            this.navigationBarState = [createDefaultBarState("navigationbar", this.navigationBarHeight)];
            this.safeAreaState = [createDefaultSafeAreaState()];
        }
        else {
            const len = this.webviews.length;
            // 同webviews的插入保持一致从前面插入
            this.statusBarState = [
                { ...this.statusBarState[0], insets: { ...this.statusBarState[0].insets } },
                ...this.statusBarState
            ];
            this.navigationBarState = [
                { ...this.navigationBarState[0], insets: { ...this.navigationBarState[0].insets } },
                ...this.navigationBarState
            ];
            this.safeAreaState = [
                { ...this.safeAreaState[0] },
                ...this.safeAreaState
            ];
        }
        return webview_id;
    }
    closeWebview(webview_id) {
        const webview = this.webviews.find((dialog) => dialog.id === webview_id);
        if (webview === undefined) {
            return false;
        }
        webview.closing = true;
        this._restateWebviews();
        return true;
    }
    closeWindow() {
        openNativeWindow_preload_js_1.mainApis.closedBrowserWindow();
    }
    _removeWebview(webview) {
        const index = this.webviews.indexOf(webview);
        if (index === -1) {
            return false;
        }
        this.webviews.splice(index, 1);
        this._restateWebviews();
        return true;
    }
    async onWebviewReady(webview, ele) {
        webview.webContentId = ele.getWebContentsId();
        webview.doReady(ele);
        openNativeWindow_preload_js_1.mainApis.denyWindowOpenHandler(webview.webContentId, (0, comlink_1.proxy)((detail) => {
            this.openWebview(detail.url);
        }));
        openNativeWindow_preload_js_1.mainApis.onDestroy(webview.webContentId, (0, comlink_1.proxy)(() => {
            this.closeWebview(webview.id);
            console.log("Destroy!!");
        }));
        ele?.addEventListener('ipc-message', this.webviewTagOnIpcMessageHandlerNormal);
    }
    async onDevtoolReady(webview, ele_devTool) {
        await webview.ready();
        if (webview.webContentId_devTools === ele_devTool.getWebContentsId()) {
            return;
        }
        webview.webContentId_devTools = ele_devTool.getWebContentsId();
        await openNativeWindow_preload_js_1.mainApis.openDevTools(webview.webContentId, undefined, webview.webContentId_devTools);
    }
    async deleteTopBarState() {
        this.statusBarState = this.statusBarState.slice(1);
        this.navigationBarState = this.navigationBarState.slice(1);
    }
    async deleteTopSafeAreaState() {
        this.safeAreaState = this.safeAreaState.slice(1);
    }
    async destroyWebview(webview) {
        console.log('destroyWebview: ');
        await openNativeWindow_preload_js_1.mainApis.destroy(webview.webContentId);
        // 还需要更新 statusbar navigationbar 和 safearea
        this.deleteTopBarState();
        this.deleteTopSafeAreaState();
    }
    async destroyWebviewByHost(host) {
        this.webviews.forEach(webview => {
            const _url = new URL(webview.src);
            if (_url.host === host) {
                this.destroyWebview(webview);
            }
        });
        return true;
    }
    async destroyWebviewByOrigin(origin) {
        console.log('destroyWebviewByOrigin: ');
        this.webviews.forEach(webview => {
            if (webview.src.includes(origin)) {
                this.destroyWebview(webview);
            }
        });
        return true;
    }
    async restartWebviewByHost(host) {
        this._restateWebviews();
        return true;
    }
    /**
     * 根据host执行 javaScript
     * @param host
     * @param code
     */
    async executeJavascriptByHost(host, code) {
        this._multiWebviewContent?.forEach(el => {
            const webview_url = new URL(el.src.split("?")[0]).origin;
            const target_url = new URL(host).origin;
            if (el.src.includes(host) || webview_url === target_url) {
                el.executeJavascript(code);
                return;
            }
        });
    }
    async acceptMessageFromWebview(options) {
        switch (options.action) {
            case "history_back":
                this.destroyWebviewByOrigin(options.origin);
                break;
            default: console.error("acceptMessageFromWebview 还有没有处理的 action = " + options.action);
        }
    }
    preloadAbsolutePathSet(path) {
        this.preloadAbsolutePath = path;
    }
    // Render the UI as a function of component state
    render() {
        const statusbarState = this.statusBarState[0];
        const navigationBarState = this.navigationBarState[0];
        const arrWebviews = this.webviews;
        return (0, lit_1.html) `
      <div class="app-container">
        <multi-webview-comp-mobile-shell
          @biometrices-pass=${() => this.biometricessPass(true)}
          @biometrices-no-pass=${() => this.biometricessPass(false)}
        >
          ${(0, repeat_js_1.repeat)(this.webviews, (webview) => webview.src, (webview, index) => {
            if (index === 0) {
                return (0, lit_1.html) `
                    <multi-webview-comp-status-bar 
                      slot="status-bar" 
                      ._color=${statusbarState.color}
                      ._style = ${statusbarState.style}
                      ._overlay = ${statusbarState.overlay}
                      ._visible = ${statusbarState.visible}
                      ._height = ${this.statusBarHeight}
                      ._inserts = ${statusbarState.insets}
                      ._torchIsOpen=${this.torchState.isOpen}
                      ._webview_src=${webview.src}
                      @safe_area_need_update=${this.safeAreaNeedUpdate}
                    ></multi-webview-comp-status-bar>
                  `;
            }
            else {
                return (0, lit_1.html) ``;
            }
        })}
          ${(0, repeat_js_1.repeat)(this.webviews, (dialog) => dialog.id, (webview) => {
            const _styleMap = (0, style_map_js_1.styleMap)({ zIndex: webview.state.zIndex + "", });
            return (0, lit_1.html) `
                <multi-webview-content
                  slot="app_content"
                  .customWebview=${webview}
                  .closing=${webview.closing}
                  .zIndex=${webview.state.zIndex}
                  .scale=${webview.state.scale}
                  .opacity=${webview.state.opacity}
                  .customWebviewId=${webview.id}
                  .src=${webview.src}
                  .preload=${this.preloadAbsolutePath}
                  style=${_styleMap}
                  @animationend=${(event) => {
                if (event.detail.event.animationName === "slideOut" && event.detail.customWebview.closing) {
                    this._removeWebview(webview);
                }
            }} 
                  @dom-ready=${(event) => {
                this.onWebviewReady(webview, event.detail.event.target);
            }}
                  data-app-url=${webview.src}
                ></multi-webview-content>
              `;
        })}
          ${(0, repeat_js_1.repeat)(this.webviews, webview => webview.src, (webview, index) => {
            if (index === 0) {
                return (0, lit_1.html) `
                    ${(0, when_js_1.when)(this.isShowVirtualKeyboard, () => (0, lit_1.html) `
                        <multi-webview-comp-virtual-keyboard
                          slot="bottom-bar"
                          ._navigation_bar_height=${this.navigationBarState[this.navigationBarState.length - 1].insets.bottom}
                          ._visible=${this.virtualKeyboardState.visible}
                          ._overlay=${this.virtualKeyboardState.overlay}
                          ._webview_src=${webview.src}
                          @first-updated=${this.virtualKeyboardFirstUpdated}
                          @hide-completed=${this.virtualKeyboardHideCompleted} 
                          @show-completed=${this.virtualKeyboardShowCompleted}
                          @height-changed=${this.virtualKeyboardStateUpdateInsetsByEl}
                        ></multi-webview-comp-virtual-keyboard>
                      `, () => {
                    const syleMap = (0, style_map_js_1.styleMap)({
                        "flex-grow": "0",
                        "flex-sharink": "0",
                        height: navigationBarState.visible ? this.navigationBarHeight : "0px"
                    });
                    return (0, lit_1.html) `
                          <multi-webview-comp-navigation-bar
                            style=${syleMap}
                            slot="bottom-bar"
                            ._color=${navigationBarState.color}
                            ._style = ${navigationBarState.style}
                            ._overlay = ${navigationBarState.overlay}
                            ._visible = ${navigationBarState.visible}
                            ._inserts = ${navigationBarState.insets}
                            ._webview_src=${webview.src}
                            @back=${this.navigationBarOnBack}
                            @safe_area_need_update=${this.safeAreaNeedUpdate}
                          ></multi-webview-comp-navigation-bar>
                        `;
                })}
                  `;
            }
            else {
                return (0, lit_1.html) ``;
            }
        })}
          
        </multi-webview-comp-mobile-shell>
      </div>
      <div class="dev-tools-container">
        ${(0, repeat_js_1.repeat)(this.webviews, (dialog) => dialog.id, (webview) => {
            const _styleMap = (0, style_map_js_1.styleMap)({ zIndex: webview.state.zIndex + "", });
            return (0, lit_1.html) `
                <multi-webview-devtools
                  .customWebview=${webview}
                  .closing=${webview.closing}
                  .zIndex=${webview.state.zIndex}
                  .scale=${webview.state.scale}
                  .opacity=${webview.state.opacity}
                  .customWebviewId=${webview.id}
                  style="${_styleMap}"
                  @dom-ready=${(event) => {
                this.onDevtoolReady(webview, event.detail.event.target);
            }}
                  @destroy-webview=${() => this.destroyWebview(webview)}
                ></multi-webview-devtools>
              `;
        })}
      </div>
    `;
    }
};
Object.defineProperty(ViewTree, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: createAllCSS()
});
__decorate([
    (0, decorators_js_1.queryAll)('multi-webview-content')
], ViewTree.prototype, "_multiWebviewContent", void 0);
__decorate([
    (0, decorators_js_1.query)('multi-webview-comp-mobile-shell')
], ViewTree.prototype, "multiWebviewCompMobileShell", void 0);
__decorate([
    (0, decorators_js_1.query)('multi-webview-comp-virtual-keyboard')
], ViewTree.prototype, "multiWebviewCompVirtualKeyboard", void 0);
__decorate([
    (0, decorators_js_1.query)('multi-webview-comp-navigation-bar')
], ViewTree.prototype, "multiWebviewCompNavigationBar", void 0);
__decorate([
    (0, decorators_js_1.property)()
], ViewTree.prototype, "name", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Object })
], ViewTree.prototype, "statusBarState", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Object })
], ViewTree.prototype, "navigationBarState", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Object })
], ViewTree.prototype, "safeAreaState", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Boolean })
], ViewTree.prototype, "isShowVirtualKeyboard", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Object })
], ViewTree.prototype, "virtualKeyboardState", void 0);
__decorate([
    (0, decorators_js_1.state)()
], ViewTree.prototype, "torchState", void 0);
__decorate([
    (0, decorators_js_1.state)()
], ViewTree.prototype, "preloadAbsolutePath", void 0);
ViewTree = __decorate([
    (0, decorators_js_1.customElement)("view-tree")
], ViewTree);
exports.ViewTree = ViewTree;
const viewTree = new ViewTree();
document.body.appendChild(viewTree);
exports.APIS = {
    openWebview: viewTree.openWebview.bind(viewTree),
    closeWebview: viewTree.closeWebview.bind(viewTree),
    closeWindow: viewTree.closeWindow.bind(viewTree),
    destroyWebviewByHost: viewTree.destroyWebviewByHost.bind(viewTree),
    restartWebviewByHost: viewTree.restartWebviewByHost.bind(viewTree),
    executeJavascriptByHost: viewTree.executeJavascriptByHost.bind(viewTree),
    acceptMessageFromWebview: viewTree.acceptMessageFromWebview.bind(viewTree),
    statusBarSetState: viewTree.barSetState.bind(viewTree, "statusBarState"),
    statusBarGetState: viewTree.barGetState.bind(viewTree, "statusBarState"),
    navigationBarSetState: viewTree.barSetState.bind(viewTree, 'navigationBarState'),
    navigationBarGetState: viewTree.barGetState.bind(viewTree, 'navigationBarState'),
    safeAreaSetOverlay: viewTree.safeAreaSetOverlay.bind(viewTree),
    safeAreaGetState: viewTree.safeAreaGetState.bind(viewTree),
    virtualKeyboardGetState: viewTree.virtualKeyboardGetState.bind(viewTree),
    virtualKeyboardSetOverlay: viewTree.virtualKeyboardSetOverlay.bind(viewTree),
    toastShow: viewTree.toastShow.bind(viewTree),
    shareShare: viewTree.shareShare.bind(viewTree),
    torchStateToggle: viewTree.torchStateToggle.bind(viewTree),
    torchStateGet: viewTree.torchStateGet.bind(viewTree),
    hapticsSet: viewTree.hapticsSet.bind(viewTree),
    biometricsMock: viewTree.biometricsMock.bind(viewTree),
    preloadAbsolutePathSet: viewTree.preloadAbsolutePathSet.bind(viewTree)
};
(0, openNativeWindow_preload_js_1.exportApis)(exports.APIS);
function createAllCSS() {
    return [
        (0, lit_1.css) `
      :host {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        width: 100%;
        height: 100%;
        background: #00000022;
      }

      .app-container{
        flex-grow: 0;
        flex-shrink: 0;
      }

      .dev-tools-container{
        flex-grow: 100;
        flex-shrink: 100;
        min-width:500px;
        height: 100%;
      }
    `
    ];
}
/**
 * 创建默认的 bar 状态
 * @param barname
 * @param height
 * @returns
 */
function createDefaultBarState(barname, height) {
    return {
        color: "#FFFFFFFF",
        style: "DEFAULT",
        insets: {
            top: barname === "statusbar" ? parseInt(height) : 0,
            right: 0,
            bottom: barname === "navigationbar" ? parseInt(height) : 0,
            left: 0
        },
        overlay: false,
        visible: true
    };
}
// 创建默认的 safearea 状态
function createDefaultSafeAreaState() {
    return {
        overlay: false
    };
}
