/// <reference lib="DOM"/>
import { proxy } from "comlink";
import { ipcRenderer } from "electron";
import { css, html, LitElement } from "lit";
import {
  customElement,
  property,
  query,
  queryAll,
  state,
} from "lit/decorators.js";
import { repeat } from "lit/directives/repeat.js";
import { styleMap } from "lit/directives/style-map.js";
import { when } from "lit/directives/when.js";
import {
  exportApis,
  mainApis,
} from "../../../helper/openNativeWindow.preload.ts";
import "./components/multi-webview-comp-barcode-scanning.html.ts";
import "./components/multi-webview-comp-biometrics.html.ts";
import "./components/multi-webview-comp-haptics.html.ts";
import "./components/multi-webview-comp-mobile-shell.html.ts";
import "./components/multi-webview-comp-navigator-bar.html.ts";
import { getButtomBarState } from "./components/multi-webview-comp-safe-area.shim.ts";
import "./components/multi-webview-comp-share.html.ts";
import "./components/multi-webview-comp-status-bar.html.ts";
import "./components/multi-webview-comp-toast.html.ts";
import "./components/multi-webview-comp-virtual-keyboard.html.ts";
import "./multi-webview-content.html.ts";
import type {
  CustomEventAnimationendDetail,
  CustomEventDomReadyDetail,
} from "./multi-webview-content.html.ts";
import "./multi-webview-devtools.html.ts";
import { Webview } from "./multi-webview.ts";
import WebviewTag = Electron.WebviewTag;
import type {
  $BarState,
  $OverlayState,
  $ShareOptions,
  $VirtualKeyboardState,
} from "../types.ts";
import type { MultiWebViewCompMobileShell } from "./components/multi-webview-comp-mobile-shell.html.ts";
import type { MultiWebviewCompNavigationBar } from "./components/multi-webview-comp-navigator-bar.html.ts";
import type { MultiWebviewCompVirtualKeyboard } from "./components/multi-webview-comp-virtual-keyboard.html.ts";
import type { MultiWebViewContent } from "./multi-webview-content.html.ts";
 
@customElement("view-tree")
export class ViewTree extends LitElement {
  static override styles = createAllCSS();
  private _id_acc = 0;
  private webviews: Array<Webview> = [];
  statusBarHeight = "38px";
  navigationBarHeight = "26px";
  virtualKeyboardAnimationSetIntervalId: unknown = 0;
  @queryAll("multi-webview-content")
  _multiWebviewContent: MultiWebViewContent[] | undefined;
  @query("multi-webview-comp-mobile-shell") multiWebviewCompMobileShell:
    | MultiWebViewCompMobileShell
    | undefined
    | null;
  @query("multi-webview-comp-virtual-keyboard")
  multiWebviewCompVirtualKeyboard: MultiWebviewCompVirtualKeyboard | undefined;
  @query("multi-webview-comp-navigation-bar") multiWebviewCompNavigationBar:
    | MultiWebviewCompNavigationBar
    | undefined;
  @property() name?: string = "Multi Webview";
  @property({ type: Object }) statusBarState: $BarState[] = [];
  @property({ type: Object }) navigationBarState: $BarState[] = [];
  @property({ type: Object }) safeAreaState: $OverlayState[] = [];
  @property({ type: Boolean }) isShowVirtualKeyboard = false;
  @property({ type: Object }) virtualKeyboardState: $VirtualKeyboardState = {
    insets: {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0,
    },
    overlay: false,
    visible: false,
  };
  @state() torchState = { isOpen: false };
  @state() preloadAbsolutePath = "";

  barSetState<
    $PropertyName extends keyof Pick<
      this,
      "statusBarState" | "navigationBarState"
    >,
    K extends keyof $BarState,
    V extends $BarState[K]
  >(propertyName: $PropertyName, key: K, value: V) {
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

  barGetState<
    $PropertyName extends keyof Pick<
      this,
      "statusBarState" | "navigationBarState"
    >
  >(propertyName: $PropertyName) {
    const i = this[propertyName].length - 1;
    return this[propertyName][i];
  }

  /**
   * 根据 multi-webview-comp-virtual-keyboard 标签
   * 和 navigationBarState.insets.bottom 的值
   * 设置 virtualKeyboardState.inserts.bottom
   */
  virtualKeyboardStateUpdateInsetsByEl() {
    const height =
      this.multiWebviewCompVirtualKeyboard?.getBoundingClientRect().height;
    if (height === undefined) throw new Error(`height === undefined`);
    const currentNavigationBarHeight =
      this.navigationBarState[this.navigationBarState.length - 1].insets.bottom;
    this.virtualKeyboardState = {
      ...this.virtualKeyboardState,
      insets: {
        ...this.virtualKeyboardState.insets,
        bottom: height <= currentNavigationBarHeight ? 0 : height,
      },
    };
    // 需要把改变发送给 sare-area
    this.safeAreaNeedUpdate();
  }

  virtualKeyboardFirstUpdated() {
    this.virtualKeyboardState = {
      ...this.virtualKeyboardState,
      visible: true,
    };
  }

  virtualKeyboardHideCompleted() {
    this.isShowVirtualKeyboard = false;
    ipcRenderer.send("safe_are_insets_change");
    clearInterval(this.virtualKeyboardAnimationSetIntervalId as number);
  }

  virtualKeyboardShowCompleted() {
    ipcRenderer.send("safe_are_insets_change");
    clearInterval(this.virtualKeyboardAnimationSetIntervalId as number);
  }

  virtualKeyboardSetOverlay(overlay: boolean) {
    const state = {
      ...this.virtualKeyboardState,
      overlay: overlay,
    };
    this.virtualKeyboardState = state;
    return state;
  }

  virtualKeyboardGetState() {
    return this.virtualKeyboardState;
  }

  toastShow(message: string, duration: string, position: "top" | "bottom") {
    this.multiWebviewCompMobileShell?.toastShow(message, duration, position);
  }

  safeAreaGetState = () => {
    const navigationBarState = this.navigationBarState[0];
    const statusbarState = this.statusBarState[0];
    const bottomBarState = getButtomBarState(
      navigationBarState,
      this.isShowVirtualKeyboard,
      this.virtualKeyboardState
    );
    return {
      overlay: this.safeAreaState[0].overlay,
      insets: {
        left: 0,
        top: statusbarState.visible
          ? statusbarState.overlay
            ? statusbarState.insets.top
            : 0
          : statusbarState.insets.top,
        right: 0,
        bottom: bottomBarState.visible
          ? bottomBarState.overlay
            ? bottomBarState.insets.bottom
            : 0
          : 0,
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
          ? statusbarState.overlay
            ? 0
            : statusbarState.insets.top
          : 0,
        right: 0,
        bottom: bottomBarState.visible
          ? bottomBarState.overlay
            ? 0
            : bottomBarState.insets.bottom
          : 0,
      },
    };
  };

  safeAreaSetOverlay = (overlay: boolean) => {
    const state = this.safeAreaState;
    state[0].overlay = overlay;
    this.safeAreaState = state;
    this.barSetState("statusBarState", "overlay", overlay);
    this.barSetState("navigationBarState", "overlay", overlay);
    this.virtualKeyboardSetOverlay(overlay);
    return this.safeAreaGetState();
  };

  safeAreaNeedUpdate = () => {
    ipcRenderer.send(
      "safe_area_update",
      new URL(this.webviews[this.webviews.length - 1].src).host.replace(
        "www.",
        "api."
      ),
      this.safeAreaGetState()
    );
  };

  torchStateToggle() {
    const state = {
      ...this.torchState,
      isOpen: !this.torchState.isOpen,
    };
    this.torchState = state;
    return state.isOpen;
  }

  torchStateGet() {
    return this.torchState.isOpen;
  }

  barcodeScanningGetPhoto() {
    const el = document.createElement("multi-webview-comp-barcode-scanning");
    document.body.append(el);
  }

  biometricesResolve: { (value: unknown): void } | undefined;
  biometricsMock() {
    this.multiWebviewCompMobileShell?.biometricsMock();
    return new Promise((resolve) => (this.biometricesResolve = resolve));
  }

  biometricessPass(b: boolean) {
    this.biometricesResolve?.(b);
  }

  hapticsSet(value: string) {
    this.multiWebviewCompMobileShell?.hapticsMock(value);
    return true;
  }

  shareShare(options: $ShareOptions) {
    this.multiWebviewCompMobileShell?.shareShare(options);
  }

  /**
   * navigation-bar 点击 back 的事件处理器
   * 业务逻辑：
   * 检查 watchers的数据是否有没返回的；
   * 
   * 如果有：
   * webveiw执行 watcher.close(); 用来发消息
   * 同时触发 close event 实际上关闭
   * 
   * 如果没有：
   * webview 执行 历史回退 或发送back 消息回来处理
   *  
   * @returns
   */
  navigationBarOnBack = () => {
    const webview = this.webviews[0];
    const origin = new URL(webview.src).origin;
    const wathcers = Array.from(this.nativeCloseWatcherKitData.tokenToId.values());
    const code = 
      wathcers.length > 0
      ?  `
          ;(() => {
            const watchers = Array.from(window.__native_close_watcher_kit__._watchers.values());
            watchers[watchers.length - 1].close();
            watchers[watchers.length - 1].dispatchEvent(new Event("close"));
          })();
        `
      : `
          history.state === null || history.state.back === null
          ? window.electron.ipcRenderer.sendToHost('back')
          : window.history.back();
          `
      ;
    this.executeJavascriptByHost(origin,code);
  };

  /** 对webview视图进行状态整理 */
  private _restateWebviews() {
    let index_acc = 0;
    let closing_acc = 0;
    let opening_acc = 0;
    let scale_sub = 0.05;
    let scale_acc = 1 + scale_sub;
    const opacity_sub = 0.1;
    let opacity_acc = 1 + opacity_sub;
    for (const webview of this.webviews) {
      webview.state.zIndex = this.webviews.length - ++index_acc;
      if (webview.closing) {
        webview.state.closingIndex = closing_acc++;
      } else {
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
  openWebview(src: string) {
    const webview_id = this._id_acc++;
    // 都从最前面插入
    this.webviews.unshift(new Webview(webview_id, src));
    this._restateWebviews();

    if (this.webviews.length === 1) {
      this.statusBarState = [
        createDefaultBarState("statusbar", this.statusBarHeight),
      ];
      this.navigationBarState = [
        createDefaultBarState("navigationbar", this.navigationBarHeight),
      ];
      this.safeAreaState = [createDefaultSafeAreaState()];
    } else {
      // 同webviews的插入保持一致从前面插入
      this.statusBarState = [
        {
          ...this.statusBarState[0],
          insets: { ...this.statusBarState[0].insets },
        },
        ...this.statusBarState,
      ];
      this.navigationBarState = [
        {
          ...this.navigationBarState[0],
          insets: { ...this.navigationBarState[0].insets },
        },
        ...this.navigationBarState,
      ];
      this.safeAreaState = [
        { ...this.safeAreaState[0] },
        ...this.safeAreaState,
      ];
    }
    return webview_id;
  }

  closeWebview(webview_id: number) {
    const webview = this.webviews.find((dialog) => dialog.id === webview_id);
    if (webview === undefined) {
      return false;
    }
    webview.closing = true;
    this._restateWebviews();
    return true;
  }

  closeWindow() {
    mainApis.closedBrowserWindow();
  }

  private _removeWebview(webview: Webview) {
    const index = this.webviews.indexOf(webview);
    if (index === -1) {
      return false;
    }
    this.webviews.splice(index, 1);
    this._restateWebviews();
    return true;
  }

  private onWebviewReady(webview: Webview, ele: WebviewTag) {
    webview.webContentId = ele.getWebContentsId();
    webview.doReady(ele);
    mainApis.denyWindowOpenHandler(
      webview.webContentId,
      proxy((detail) => {
        this.openWebview(detail.url);
      })
    );
    mainApis.onDestroy(
      webview.webContentId,
      proxy(() => {
        this.closeWebview(webview.id);
        console.log("Destroy!!");
      })
    );

    mainApis.openDevToolsAtBrowserWindowByWebContentsId(
      webview.webContentId,
      webview.src
    );
    console.log("weview: ", webview)

    ele?.addEventListener(
      "ipc-message",
      this.webviewTagOnIpcMessageHandlerNormal
    );
  }

  private async onDevtoolReady(webview: Webview, ele_devTool: WebviewTag) {
    await webview.ready();
    if (webview.webContentId_devTools === ele_devTool.getWebContentsId()) {
      return;
    }
    webview.webContentId_devTools = ele_devTool.getWebContentsId();
    await mainApis.openDevTools(
      webview.webContentId,
      undefined,
      webview.webContentId_devTools
    );
  }

  private deleteTopBarState() {
    this.statusBarState = this.statusBarState.slice(1);
    this.navigationBarState = this.navigationBarState.slice(1);
  }

  private deleteTopSafeAreaState() {
    this.safeAreaState = this.safeAreaState.slice(1);
  }

  private async destroyWebview(webview: Webview) {
    console.log("destroyWebview: ");

    await mainApis.destroy(webview.webContentId);
    // 还需要更新 statusbar navigationbar 和 safearea
    this.deleteTopBarState();
    this.deleteTopSafeAreaState();
  }

  destroyWebviewByHost(host: string) {
    this.webviews.forEach((webview) => {
      const _url = new URL(webview.src);
      if (_url.host === host) {
        this.destroyWebview(webview);
      }
    });
    return true;
  }

  destroyWebviewByOrigin(origin: string) {
    console.log("destroyWebviewByOrigin: ");
    this.webviews.forEach((webview) => {
      if (webview.src.includes(origin)) {
        this.destroyWebview(webview);
      }
    });
    return true;
  }

  restartWebviewByHost(_host: string) {
    this._restateWebviews();
    return true;
  }

  /**
   * 根据host执行 javaScript
   * @param host
   * @param code
   */
  executeJavascriptByHost(host: string, code: string) {
    this._multiWebviewContent?.forEach((el) => {
      const webview_url = new URL(el.src.split("?")[0]).origin;
      const target_url = new URL(host).origin;
      if (el.src.includes(host) || webview_url === target_url) {
        el.executeJavascript(code);
        return;
      }
    });
  }

  preloadAbsolutePathSet(path: string) {
    this.preloadAbsolutePath = path;
  }

  webviewTagOnIpcMessageHandlerBack = () => {
    const len = this.webviews.length;
    if(len > 1){
      this.destroyWebview(this.webviews[0]);
      this.navigationBarState = this.navigationBarState.slice(1);
      this.statusBarState = this.statusBarState.slice(1);
      // 把 navigationBarState statusBarStte safe-area 的改变发出
      ipcRenderer.send("safe_are_insets_change");
      ipcRenderer.send("navigation_bar_state_change");
      ipcRenderer.send("status_bar_state_change");
      return;
    }
    console.error('是否应该需要关闭 当前window了？？？ 还没有决定')
    mainApis.closedBrowserWindow()
  };

  webviewTagOnIpcMessageHandlerNormal = (e: Event) => {
    const channel = Reflect.get(e, "channel");
    const args = Reflect.get(e, "args");
    switch (channel) {
      case "virtual_keyboard_open":
        this.isShowVirtualKeyboard = true;
        break;
      case "virtual_keyboard_close":
        this.virtualKeyboardState = {
          ...this.virtualKeyboardState,
          visible: false,
        };
        break;
      case "back":
        this.webviewTagOnIpcMessageHandlerBack();
        break;
      case "__native_close_watcher_kit__":
        this.nativeCloseWatcherKit((args as {action: string, value: string | number}[])[0])
        break;
      default:
        throw new Error(
          `webview ipc-message 还有没有处理的channel===${channel}`
        );
    }
  };

  nativeCloseWatcherKitData = {
    tokenToId: new Map<string, number>(),
    idToToken: new Map<number, string>(),
    allocId: 0,
  }
  nativeCloseWatcherKit = ({action, value}:  {action: string, value: string | number}) => {
    console.log('接受的到了消息： ', action, value)
    
    if(action === "registry_token" && typeof value === "string"){
      const id = this.nativeCloseWatcherKitData.allocId++;
      this.nativeCloseWatcherKitData.tokenToId.set(value, id);
      this.nativeCloseWatcherKitData.idToToken.set(id, value)
      // 向 webview 执行数据 resolve watcher
      const webview = this.webviews[0];
      const origin = new URL(webview.src).origin;
      this.executeJavascriptByHost(
        origin,
        `
          ;(() => {
            console.log(globalThis.__native_close_watcher_kit__._tasks);
            const resolve = globalThis.__native_close_watcher_kit__._tasks.get("${value}");
            resolve(${id});
          })();
        `
      )
      return;
    }

    if(action === "close" && typeof value === "number"){
      const token = this.nativeCloseWatcherKitData.idToToken.get(value)!
      this.nativeCloseWatcherKitData.idToToken.delete(value);
      this.nativeCloseWatcherKitData.tokenToId.delete(token);
      console.log("接受到了关闭的消息", value)
      return;
    }
  }

  // Render the UI as a function of component state
  override render() {
    const statusbarState = this.statusBarState[0];
    const navigationBarState = this.navigationBarState[0];
    return html`
      <div class="app-container">
        <multi-webview-comp-mobile-shell
          @biometrices-pass=${() => this.biometricessPass(true)}
          @biometrices-no-pass=${() => this.biometricessPass(false)}
        >
          ${repeat(
            this.webviews,
            (webview) => webview.src,
            (webview, index) => {
              if (index === 0) {
                return html`
                  <multi-webview-comp-status-bar
                    slot="status-bar"
                    ._color=${statusbarState.color}
                    ._style=${statusbarState.style}
                    ._overlay=${statusbarState.overlay}
                    ._visible=${statusbarState.visible}
                    ._height=${this.statusBarHeight}
                    ._inserts=${statusbarState.insets}
                    ._torchIsOpen=${this.torchState.isOpen}
                    ._webview_src=${webview.src}
                    @safe_area_need_update=${this.safeAreaNeedUpdate}
                  ></multi-webview-comp-status-bar>
                `;
              } else {
                return html``;
              }
            }
          )}
          ${repeat(
            this.webviews,
            (dialog) => dialog.id,
            (webview) => {
              const _styleMap = styleMap({ zIndex: webview.state.zIndex + "" });
              return html`
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
                  @animationend=${(
                    event: CustomEvent<CustomEventAnimationendDetail>
                  ) => {
                    if (
                      event.detail.event.animationName === "slideOut" &&
                      event.detail.customWebview.closing
                    ) {
                      this._removeWebview(webview);
                    }
                  }}
                  @dom-ready=${(
                    event: CustomEvent<CustomEventDomReadyDetail>
                  ) => {
                    this.onWebviewReady(
                      webview,
                      event.detail.event.target as WebviewTag
                    );
                  }}
                  data-app-url=${webview.src}
                ></multi-webview-content>
              `;
            }
          )}
          ${repeat(
            this.webviews,
            (webview) => webview.src,
            (webview, index) => {
              if (index === 0) {
                return html`
                  ${when(
                    this.isShowVirtualKeyboard,
                    () => html`
                      <multi-webview-comp-virtual-keyboard
                        slot="bottom-bar"
                        ._navigation_bar_height=${this.navigationBarState[
                          this.navigationBarState.length - 1
                        ].insets.bottom}
                        ._visible=${this.virtualKeyboardState.visible}
                        ._overlay=${this.virtualKeyboardState.overlay}
                        ._webview_src=${webview.src}
                        @first-updated=${this.virtualKeyboardFirstUpdated}
                        @hide-completed=${this.virtualKeyboardHideCompleted}
                        @show-completed=${this.virtualKeyboardShowCompleted}
                        @height-changed=${this
                          .virtualKeyboardStateUpdateInsetsByEl}
                      ></multi-webview-comp-virtual-keyboard>
                    `,
                    () => {
                      const syleMap = styleMap({
                        "flex-grow": "0",
                        "flex-sharink": "0",
                        height: navigationBarState.visible
                          ? this.navigationBarHeight
                          : "0px",
                      });
                      return html`
                        <multi-webview-comp-navigation-bar
                          style=${syleMap}
                          slot="bottom-bar"
                          ._color=${navigationBarState.color}
                          ._style=${navigationBarState.style}
                          ._overlay=${navigationBarState.overlay}
                          ._visible=${navigationBarState.visible}
                          ._inserts=${navigationBarState.insets}
                          ._webview_src=${webview.src}
                          @back=${this.navigationBarOnBack}
                          @safe_area_need_update=${this.safeAreaNeedUpdate}
                        ></multi-webview-comp-navigation-bar>
                      `;
                    }
                  )}
                `;
              } else {
                return html``;
              }
            }
          )}
        </multi-webview-comp-mobile-shell>
      </div>
      <!--
      <div class="dev-tools-container">
        ${repeat(
          this.webviews,
          (dialog) => dialog.id,
          (webview) => {
            const _styleMap = styleMap({ zIndex: webview.state.zIndex + "" });
            return html`
              <multi-webview-devtools
                .customWebview=${webview}
                .closing=${webview.closing}
                .zIndex=${webview.state.zIndex}
                .scale=${webview.state.scale}
                .opacity=${webview.state.opacity}
                .customWebviewId=${webview.id}
                style="${_styleMap}"
                @dom-ready=${(event: CustomEvent & { target: WebviewTag }) => {
                  this.onDevtoolReady(
                    webview,
                    event.detail.event.target as WebviewTag
                  );
                }}
                @destroy-webview=${() => this.destroyWebview(webview)}
              ></multi-webview-devtools>
            `;
          }
        )}
      </div>
      -->
    `;
  }
}

const viewTree = new ViewTree();
document.body.appendChild(viewTree);

export const APIS = {
  openWebview: viewTree.openWebview.bind(viewTree),
  closeWebview: viewTree.closeWebview.bind(viewTree),
  closeWindow: viewTree.closeWindow.bind(viewTree),
  destroyWebviewByHost: viewTree.destroyWebviewByHost.bind(viewTree),
  restartWebviewByHost: viewTree.restartWebviewByHost.bind(viewTree),
  executeJavascriptByHost: viewTree.executeJavascriptByHost.bind(viewTree),
  statusBarSetState: viewTree.barSetState.bind(viewTree, "statusBarState"),
  statusBarGetState: viewTree.barGetState.bind(viewTree, "statusBarState"),
  navigationBarSetState: viewTree.barSetState.bind(
    viewTree,
    "navigationBarState"
  ),
  navigationBarGetState: viewTree.barGetState.bind(
    viewTree,
    "navigationBarState"
  ),
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
  preloadAbsolutePathSet: viewTree.preloadAbsolutePathSet.bind(viewTree),
};

exportApis(APIS);

function createAllCSS() {
  return [
    css`
      :host {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        width: 100%;
        height: 100%;
        background: #00000022;
      }

      .app-container {
        flex-grow: 0;
        flex-shrink: 0;
        width: 100%;
        height: 100%;
      }

      .dev-tools-container {
        flex-grow: 100;
        flex-shrink: 100;
        min-width: 500px;
        height: 100%;
      }
    `,
  ];
}

/**
 * 创建默认的 bar 状态
 * @param barname
 * @param height
 * @returns
 */
function createDefaultBarState(
  barname: "statusbar" | "navigationbar",
  height: string
): $BarState {
  return {
    color: "#FFFFFFFF",
    style: "DEFAULT",
    insets: {
      top: barname === "statusbar" ? parseInt(height) : 0,
      right: 0,
      bottom: barname === "navigationbar" ? parseInt(height) : 0,
      left: 0,
    },
    overlay: false,
    visible: true,
  };
}

// 创建默认的 safearea 状态
function createDefaultSafeAreaState(): $OverlayState {
  return {
    overlay: false,
  };
}
