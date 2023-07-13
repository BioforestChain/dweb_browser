import { BiometricsController } from "./controller/biometrics.controller.ts";
import { StatusBarController } from "./controller/status-bar.controller.ts";
// 测试入口文件
import { BaseController } from "./controller/base-controller.ts";
import { HapticsController } from "./controller/haptics.controller.ts";
import { NavigationBarController } from "./controller/navigation-bar.controller.ts";
import { TorchController } from "./controller/torch.controller.ts";
import { VirtualKeyboardController } from "./controller/virtual-keyboard.controller.ts";
import "./emulator-toolbar.html.ts";
import { css, customElement, html, LitElement, property, query, state, when } from "./helper/litHelper.ts";
import "./multi-webview-comp-biometrics.html.ts";
import "./multi-webview-comp-haptics.html.ts";
import "./multi-webview-comp-mobile-shell.html.ts";
import "./multi-webview-comp-navigator-bar.html.ts";
import "./multi-webview-comp-share.html.ts";
import "./multi-webview-comp-status-bar.html.ts";
import "./multi-webview-comp-toast.html.ts";
import "./multi-webview-comp-virtual-keyboard.html.ts";

const TAG = "root-comp";

@customElement(TAG)
export class RootComp extends LitElement {
  @property({ type: String }) src = "about:blank";
  static override styles = createAllCSS();
  @state() controllers = new Set<BaseController>();
  private _wc<C extends BaseController>(c: C) {
    c.onUpdate(() => this.requestUpdate())
      .onInit((c) => {
        this.controllers.add(c);
        this.requestUpdate();
      })
      .onReady((c) => {
        this.controllers.delete(c);
        this.requestUpdate();
      });
    return c;
  }
  @query("iframe") iframeEle?: HTMLIFrameElement;
  private _bindReloadShortcut = () => {
    debugger;
    this.iframeEle?.contentWindow?.addEventListener("keydown", (e) => {
      e = e || window.event;
      if (
        (e.ctrlKey && e.keyCode == 82) || //ctrl+R
        e.keyCode == 116
      ) {
        debugger;
        this.iframeEle?.contentWindow?.location.reload();
        //F5刷新，禁止
      }
    });
  };

  /**statusBar */
  readonly statusBarController = this._wc(new StatusBarController());

  get statusBarState() {
    return this.statusBarController.state;
  }
  /**navigationBar */
  readonly navigationController = this._wc(new NavigationBarController());

  get navigationBarState() {
    return this.navigationController.state;
  }
  /**virtualboard */
  readonly virtualKeyboardController = this._wc(new VirtualKeyboardController());

  get virtualKeyboardState() {
    return this.virtualKeyboardController.state;
  }

  readonly torchController = this._wc(new TorchController());

  get torchState() {
    return this.torchController.state;
  }

  readonly hapticsController = this._wc(new HapticsController());
  readonly biometricsController = this._wc(new BiometricsController());

  protected override render() {
    return html`
      <div class="root">
        <emulator-toolbar .url=${this.src}></emulator-toolbar>

        <multi-webview-comp-mobile-shell class="main-view">
          ${when(this.biometricsController.state, () => {
            const state = this.biometricsController.state!;
            html`<multi-webview-comp-biometrics
              @pass=${state.resolve({ success: true, message: "okk" })}
              @no-pass=${state.resolve({ success: false, message: "...." })}
            ></multi-webview-comp-biometrics>`;
          })}
          <multi-webview-comp-status-bar
            slot="status-bar"
            ._color=${this.statusBarState.color}
            ._style=${this.statusBarState.style}
            ._overlay=${this.statusBarState.overlay}
            ._visible=${this.statusBarState.visible}
            ._insets=${this.statusBarState.insets}
            ._torchIsOpen=${this.torchState.isOpen}
          ></multi-webview-comp-status-bar>
          ${when(
            this.controllers.size === 0,
            () => html`
              <iframe
                referrerpolicy="no-referrer"
                slot="shell-content"
                style="width:100%;height:100%;border:0;"
                src=${this.src}
                @loadstart=${this._bindReloadShortcut}
              ></iframe>
            `,
            () => html`<div class="boot-logo" slot="shell-content">开机中</div>`
          )}
          ${when(
            this.virtualKeyboardController.isShowVirtualKeyboard,
            () => html`
              <multi-webview-comp-virtual-keyboard
                slot="bottom-bar"
                ._visible=${this.virtualKeyboardState.visible}
                ._overlay=${this.virtualKeyboardState.overlay}
                @first-updated=${this.virtualKeyboardController.virtualKeyboardFirstUpdated}
                @hide-completed=${this.virtualKeyboardController.virtualKeyboardHideCompleted}
                @show-completed=${this.virtualKeyboardController.virtualKeyboardShowCompleted}
              ></multi-webview-comp-virtual-keyboard>
            `,
            () => {
              return html`
                <multi-webview-comp-navigation-bar
                  slot="bottom-bar"
                  ._color=${this.navigationBarState.color}
                  ._style=${this.navigationBarState.style}
                  ._overlay=${this.navigationBarState.overlay}
                  ._visible=${this.navigationBarState.visible}
                  ._inserts=${this.navigationBarState.insets}
                ></multi-webview-comp-navigation-bar>
              `;
            }
          )}
        </multi-webview-comp-mobile-shell>
      </div>
    `;
  }
}

// 生成 CSS 文件 给 styles
function createAllCSS() {
  return [
    css`
      :host {
        display: block;
      }
      .root {
        display: flex;
        flex-direction: column;
        height: 100%;
      }
      .main-view {
        flex: 1;
      }

      .boot-logo {
        height: 100%;
        display: grid;
        place-items: center;
        font-size: 32px;
        color: rgba(255, 255, 255, 0.3);
        background: linear-gradient(
            -30deg,
            rgba(255, 255, 255, 0) 100px,
            rgba(255, 255, 255, 1) 180px,
            rgba(255, 255, 255, 1) 240px,
            rgba(255, 255, 255, 0) 300px
          ) -300px 0 no-repeat;
        -webkit-background-clip: text;
        animation-name: boot-logo;
        animation-duration: 6000ms;
        animation-iteration-count: infinite;
        /* animation-fill-mode:forward */
      }
      @keyframes boot-logo {
        0% {
          background-position: -300px 0px;
        }
        100% {
          background-position: 1000px 0px;
        }
      }
    `,
  ];
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: RootComp;
  }
}
