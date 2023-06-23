/// <reference lib="DOM"/>
import { css, html } from "lit";
import { customElement, queryAll } from "lit/decorators.js";
import { repeat } from "lit/directives/repeat.js";
import { styleMap } from "lit/directives/style-map.js";
import { when } from "lit/directives/when.js";
import "../../../helper/electron.ts";
import {
  exportApis,
  mainApis,
} from "../../../helper/openNativeWindow.preload.ts";
import { NativeUI } from "./appSheel.nativeui.ts";
import "./components/multi-webview-comp-barcode-scanning.html.ts";
import "./components/multi-webview-comp-biometrics.html.ts";
import "./components/multi-webview-comp-haptics.html.ts";
import "./components/multi-webview-comp-mobile-shell.html.ts";
import "./components/multi-webview-comp-navigator-bar.html.ts";
import "./components/multi-webview-comp-share.html.ts";
import "./components/multi-webview-comp-status-bar.html.ts";
import "./components/multi-webview-comp-toast.html.ts";
import "./components/multi-webview-comp-virtual-keyboard.html.ts";
import "./viewItem.html.ts";
import type {
  CustomEventAnimationendDetail,
  CustomEventDomReadyDetail,
} from "./viewItem.html.ts";
import { MultiWebViewContent } from "./viewItem.html.ts";
import { ViewItem } from "./viewItem.ts";
import WebviewTag = Electron.WebviewTag;

const TAG = "mwebview-app-shell";

@customElement(TAG)
export class AppShellElement extends NativeUI {
  static styles = [
    css`
      :host {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        width: 100%;
        height: 100%;
      }

      .device-container {
        flex-grow: 0;
        flex-shrink: 0;
        width: 100%;
        height: 100%;

        display: flex;
        flex-direction: column;
      }

      .device-container > .device {
        flex: 1;
        /* box-shadow: 0 0 0.5em rgba(0, 0, 0, 0.3); */
      }

      .dev-tools-container {
        flex-grow: 100;
        flex-shrink: 100;
        min-width: 500px;
        height: 100%;
      }
    `,
  ];

  @queryAll("mwebview-view-item")
  _multiWebviewContent?: MultiWebViewContent[];

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
  navigationBarOnBack = async () => {
    const viewItem = this.viewItems[0];
    const origin = new URL(viewItem.src).origin;
    const wathcers = Array.from(
      this.nativeCloseWatcherKitData.tokenToId.values()
    );
    const webview = await viewItem.ready();
    if (wathcers.length > 0) {
      const code = `
      ;(() => {
        const watchers = Array.from(window.__native_close_watcher_kit__._watchers.values());
        const closeWatcher = watchers.at(-1);
        closeWatcher.close();
        closeWatcher.dispatchEvent(new Event("close"));
      })`;
      await webview.executeJavaScript(code);
    } else {
      webview.goBack();
    }
  };

  webviewTag_onIpcMessage_back = () => {
    const len = this.viewItems.length;
    if (len > 1) {
      this.webveiws_deleteById(this.viewItems[0].id);
      return;
    }
    console.error("是否应该需要关闭 当前window了？？？ 还没有决定");
    mainApis.closedBrowserWindow();
  };

  webviewTag_onDomReady = async (webview: ViewItem, ele: WebviewTag) => {
    webview.webContentId = ele.getWebContentsId();
    webview.doReady(ele);

    // 打开devtools
    await mainApis.openDevToolsWindowAsFollower(
      webview.webContentId,
      webview.src
    );
  };
  // Render the UI as a function of component state
  override render() {
    const _webveiw = this.viewItems[0];
    if (_webveiw === undefined) return null;
    const statusbarState = this.statusBarState.get(_webveiw);
    if (statusbarState === undefined)
      throw new Error(`statusbarState === undefined`);
    const navigationBarState = this.navigationBarState.get(_webveiw);
    if (navigationBarState === undefined)
      throw new Error(`navigationBarState === undefined`);
    return html`
      <div class="device-container">
        <multi-webview-comp-mobile-shell
          class="device"
          @biometrices-pass=${() => this.biometricessPass(true)}
          @biometrices-no-pass=${() => this.biometricessPass(false)}
        >
          ${repeat(
            this.viewItems,
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
            this.viewItems,
            (dialog) => dialog.id,
            (viewItem) => {
              const _styleMap = styleMap({
                zIndex: viewItem.state.zIndex + "",
              });
              return html`
                <mwebview-view-item
                  slot="app_content"
                  .closing=${viewItem.closing}
                  .zIndex=${viewItem.state.zIndex}
                  .scale=${viewItem.state.scale}
                  .opacity=${viewItem.state.opacity}
                  .customWebviewId=${viewItem.id}
                  .src=${viewItem.src}
                  .preload=${this.preloadAbsolutePath}
                  style=${_styleMap}
                  @animationend=${(
                    event: CustomEvent<CustomEventAnimationendDetail>
                  ) => {
                    if (
                      event.detail.event.animationName === "slideOut" &&
                      viewItem.closing
                    ) {
                      // this._removeWebview(webview);
                      this.webveiws_deleteById(viewItem.id);
                    }
                  }}
                  @dom-ready=${(
                    event: CustomEvent<CustomEventDomReadyDetail>
                  ) => {
                    this.webviewTag_onDomReady(
                      viewItem,
                      event.detail.event.target as WebviewTag
                    );
                  }}
                  data-app-url=${viewItem.src}
                ></mwebview-view-item>
              `;
            }
          )}
          ${repeat(
            this.viewItems,
            (webview) => webview.src,
            (webview, index) => {
              if (index === 0) {
                return html`
                  ${when(
                    this.isShowVirtualKeyboard,
                    () => html`
                      <multi-webview-comp-virtual-keyboard
                        slot="bottom-bar"
                        ._navigation_bar_height=${navigationBarState.insets
                          .bottom}
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
    `;
  }
}

export const appShell = new AppShellElement();
document.body.appendChild(appShell);
exportApis(appShell);
