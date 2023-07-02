import { BiometricsController } from "./controller/biometrics.controller.ts";
import { StatusBarController } from "./controller/status-bar.controller.ts";
// 测试入口文件
import { css, html, LitElement } from "lit";
import { customElement, query } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import { HapticsController } from "./controller/haptics.controller.ts";
import { NavigationBarController } from "./controller/navigation-bar.controller.ts";
import { TorchController } from "./controller/torch.controller.ts";
import { VirtualKeyboardController } from "./controller/virtual-keyboard.controller.ts";
import "./multi-webview-comp-biometrics.html.ts";
import "./multi-webview-comp-haptics.html.ts";
import "./multi-webview-comp-mobile-shell.html.ts";
import type { MultiWebViewCompMobileShell } from "./multi-webview-comp-mobile-shell.html.ts";
import "./multi-webview-comp-navigator-bar.html.ts";
import "./multi-webview-comp-share.html.ts";
import "./multi-webview-comp-status-bar.html.ts";
import "./multi-webview-comp-toast.html.ts";
import "./multi-webview-comp-virtual-keyboard.html.ts";
import type { $ShareOptions } from "./types.ts";

const TAG = "root-comp";

@customElement(TAG)
export class RootComp extends LitElement {
  static override styles = createAllCSS();

  @query("multi-webview-comp-mobile-shell")
  mobileShell: MultiWebViewCompMobileShell | null = null;

  /**statusBar */
  readonly statusBarController = new StatusBarController().onUpdate(() => {
    this.requestUpdate();
  });
  get statusBarState() {
    return this.statusBarController.state;
  }
  /**navigationBar */
  readonly navigationController = new NavigationBarController().onUpdate(() => {
    this.requestUpdate();
  });
  get navigationBarState() {
    return this.navigationController.state;
  }
  /**virtualboard */
  readonly virtualKeyboardController = new VirtualKeyboardController().onUpdate(
    () => this.requestUpdate()
  );
  get virtualKeyboardState() {
    return this.virtualKeyboardController.state;
  }

  readonly torchController = new TorchController().onUpdate(() => {
    this.requestUpdate();
  });
  get torchState() {
    return this.torchController.state;
  }

  readonly hapticsController = new HapticsController(this.mobileShell);
  readonly biometricsController = new BiometricsController().onUpdate(() =>
    this.requestUpdate()
  );

  shareShare(options: $ShareOptions) {
    this.mobileShell?.shareShare(options);
  }

  protected override render() {
    return html`
      <multi-webview-comp-mobile-shell>
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
        <slot slot="shell-content"></slot>
        ${when(
          this.virtualKeyboardController.isShowVirtualKeyboard,
          () => html`
            <multi-webview-comp-virtual-keyboard
              slot="bottom-bar"
              ._visible=${this.virtualKeyboardState.visible}
              ._overlay=${this.virtualKeyboardState.overlay}
              @first-updated=${this.virtualKeyboardController
                .virtualKeyboardFirstUpdated}
              @hide-completed=${this.virtualKeyboardController
                .virtualKeyboardHideCompleted}
              @show-completed=${this.virtualKeyboardController
                .virtualKeyboardShowCompleted}
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
    `,
  ];
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: RootComp;
  }
}
