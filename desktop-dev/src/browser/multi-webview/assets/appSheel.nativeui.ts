import { query } from "lit/decorators.js";
import type { $ShareOptions } from "../types.ts";
import { Api } from "./appSheel.api.ts";
import type { MultiWebViewCompMobileShell } from "./components/multi-webview-comp-mobile-shell.html.ts";
import type { MultiWebviewCompNavigationBar } from "./components/multi-webview-comp-navigator-bar.html.ts";
import type { MultiWebviewCompVirtualKeyboard } from "./components/multi-webview-comp-virtual-keyboard.html.ts";

export class NativeUI extends Api {
  @query("multi-webview-comp-mobile-shell")
  multiWebviewCompMobileShell?: MultiWebViewCompMobileShell;
  @query("multi-webview-comp-virtual-keyboard")
  multiWebviewCompVirtualKeyboard?: MultiWebviewCompVirtualKeyboard;
  @query("multi-webview-comp-navigation-bar")
  multiWebviewCompNavigationBar?: MultiWebviewCompNavigationBar;

  //#region 虚拟键盘

  /**
   * 根据 multi-webview-comp-virtual-keyboard 标签
   * 和 navigationBarState.insets.bottom 的值
   * 设置 virtualKeyboardState.inserts.bottom
   */
  virtualKeyboardStateUpdateInsetsByEl = () => {
    const height =
      this.multiWebviewCompVirtualKeyboard?.getBoundingClientRect().height;
    if (height === undefined) throw new Error(`height === undefined`);
    const currentNavigationBarHeight = this.navigationBarState.get(
      this.viewItems[0]
    )?.insets.bottom;
    if (currentNavigationBarHeight === undefined)
      throw new Error(`currentNavigationBarHeight === undefined`);
    this.virtualKeyboardState = {
      ...this.virtualKeyboardState,
      insets: {
        ...this.virtualKeyboardState.insets,
        bottom: height <= currentNavigationBarHeight ? 0 : height,
      },
    };
    // 需要把改变发送给 sare-area
    this.safeAreaNeedUpdate();
  };
  //#endregion

  //#region 生物识别

  biometricesResolve: { (value: boolean): void } | undefined;
  biometricessPass(b: boolean) {
    this.biometricesResolve?.(b);
  }

  biometricsMock() {
    this.multiWebviewCompMobileShell?.biometricsMock();
    return new Promise<boolean>(
      (resolve) => (this.biometricesResolve = resolve)
    );
  }

  hapticsSet(value: string) {
    this.multiWebviewCompMobileShell?.hapticsMock(value);
    return true;
  }
  //#endregion

  //#region 分享

  shareShare(options: $ShareOptions) {
    this.multiWebviewCompMobileShell?.shareShare(options);
  }
  //#endregion

  //#region 弹出

  toastShow(message: string, duration: string, position: "top" | "bottom") {
    this.multiWebviewCompMobileShell?.toastShow(message, duration, position);
  }
  //#endregion
}
