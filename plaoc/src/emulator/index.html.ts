import { StatusBarController } from "./controller/status-bar.controller.ts";
// 测试入口文件
import { css, html, LitElement } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import "./multi-webview-comp-barcode-scanning.html.ts";
import "./multi-webview-comp-biometrics.html.ts";
import "./multi-webview-comp-haptics.html.ts";
import "./multi-webview-comp-mobile-shell.html.ts";
import type { MultiWebViewCompMobileShell } from "./multi-webview-comp-mobile-shell.html.ts";
import "./multi-webview-comp-navigator-bar.html.ts";
import { getButtomBarState } from "./multi-webview-comp-safe-area.shim.ts";
import "./multi-webview-comp-share.html.ts";
import "./multi-webview-comp-status-bar.html.ts";
import "./multi-webview-comp-toast.html.ts";
import "./multi-webview-comp-virtual-keyboard.html.ts";
import type { $BAR_STYLE, $BarState, $ShareOptions } from "./types.ts";

const TAG = "root-comp";

@customElement(TAG)
export class RootComp extends LitElement {
  static override styles = createAllCSS();

  @query("multi-webview-comp-mobile-shell") multiWebviewCompMobileShell:
    | MultiWebViewCompMobileShell
    | undefined
    | null;
  statusBarHeight = "38px";
  readonly statusBarController = new StatusBarController().onUpdate(() => {
    this.requestUpdate();
  });
  get statusBarState() {
    return this.statusBarController.state;
  }

  navigationBarHeight = "26px";
  @property({ type: Object }) navigationBarState: $BarState = {
    color: "#FFFFFFFF",
    style: "DEFAULT",
    insets: {
      top: 0,
      right: 0,
      bottom: parseInt(this.navigationBarHeight),
      left: 0,
    },
    overlay: false,
    visible: true,
  };
  @property({ type: Object }) safeAreaState = {
    overlay: false,
  };
  @property({ type: Boolean }) isShowVirtualKeyboard = false;
  @property({ type: Object }) virtualKeyboardState = {
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

  navigationBarSetStyle(style: $BAR_STYLE) {
    this.navigationBarState = {
      ...this.navigationBarState,
      style: style,
    };
    return this;
  }

  navigationBarSetBackground(color: string) {
    this.navigationBarState = {
      ...this.navigationBarState,
      color: color,
    };
    return this;
  }

  navigationBarSetOverlay(overlay: boolean) {
    this.navigationBarState = {
      ...this.navigationBarState,
      overlay: overlay,
    };
    return this;
  }

  navigationBarSetVisible(visible: boolean) {
    this.navigationBarState = {
      ...this.navigationBarState,
      visible: visible,
    };
    return this;
  }

  async navigationBarGetState() {
    return {
      ...this.navigationBarState,
      color: this.hexaToRGBA(this.navigationBarState.color),
    };
  }

  virtualKeyboardSetOverlay(overlay: boolean) {
    this.virtualKeyboardState = {
      ...this.virtualKeyboardState,
      overlay: overlay,
    };
    return this;
  }

  hexaToRGBA(str: string) {
    return {
      red: parseInt(str.slice(1, 3), 16),
      green: parseInt(str.slice(3, 5), 16),
      blue: parseInt(str.slice(5, 7), 16),
      alpha: parseInt(str.slice(7), 16),
    };
  }

  virtualKeyboardFirstUpdated() {
    this.virtualKeyboardState = {
      ...this.virtualKeyboardState,
      visible: true,
    };
  }

  virtualKeyboardHideCompleted() {
    this.isShowVirtualKeyboard = false;
    console.error(`virtualKeybark 隐藏完成了 但是还没有处理`);
  }

  virtualKeyboardShowCompleted() {
    console.error("virutalKeyboard 显示完成了 但是还没有处理");
  }

  safeAreaGetState = () => {
    const bottomBarState = getButtomBarState(
      this.navigationBarState,
      this.isShowVirtualKeyboard,
      this.virtualKeyboardState
    );

    return {
      overlay: this.safeAreaState.overlay,
      insets: {
        left: 0,
        top: this.statusBarState.overlay ? this.statusBarState.insets.top : 0,
        right: 0,
        bottom: bottomBarState.overlay ? bottomBarState.insets.bottom : 0,
      },
      cutoutInsets: {
        left: 0,
        top: this.statusBarState.insets.top,
        right: 0,
        bottom: 0,
      },
      // 外部尺寸
      outerInsets: {
        left: 0,
        top: this.statusBarState.overlay ? 0 : this.statusBarState.insets.top,
        right: 0,
        bottom: bottomBarState.overlay ? 0 : bottomBarState.insets.bottom,
      },
    };
  };

  safeAreaSetOverlay = (overlay: boolean) => {
    this.statusBarController.statusBarSetOverlay(overlay);
    this.navigationBarSetOverlay(overlay);
    this.virtualKeyboardSetOverlay(overlay);
  };

  torchToggleTorch() {
    this.torchState = {
      ...this.torchState,
      isOpen: !this.torchState.isOpen,
    };
    return this;
  }

  barcodeScanningGetPhoto() {
    const el = document.createElement("multi-webview-comp-barcode-scanning");
    document.body.append(el);
  }

  biometricsMock() {
    this.multiWebviewCompMobileShell?.biometricsMock();
  }

  hapticsMock() {
    this.multiWebviewCompMobileShell?.hapticsMock("HEAVY");
  }

  shareShare(options: $ShareOptions) {
    this.multiWebviewCompMobileShell?.shareShare(options);
  }

  // 之后的方法全部都是 测试代码
  virtualKeyboardShow() {
    this.isShowVirtualKeyboard = true;
  }

  virtualKeyboardHide() {
    this.virtualKeyboardState = {
      ...this.virtualKeyboardState,
      visible: false,
    };
  }

  allocId = 0;
  toast() {
    this.multiWebviewCompMobileShell?.toastShow(
      "message" + this.allocId++,
      `1000`,
      "top"
    );
  }

  // 测试代码结束

  protected override render() {
    return html`
      <multi-webview-comp-mobile-shell>
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
          this.isShowVirtualKeyboard,
          () => html`
            <multi-webview-comp-virtual-keyboard
              slot="bottom-bar"
              ._visible=${this.virtualKeyboardState.visible}
              ._overlay=${this.virtualKeyboardState.overlay}
              @first-updated=${this.virtualKeyboardFirstUpdated}
              @hide-completed=${this.virtualKeyboardHideCompleted}
              @show-completed=${this.virtualKeyboardShowCompleted}
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
                ._height=${this.navigationBarHeight}
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
