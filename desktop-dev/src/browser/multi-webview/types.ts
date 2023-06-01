/**
 * Light text for dark backgrounds.
 * "DARK"
 *
 * Dark text for light backgrounds.
 * "LIGHT",
 *
 * The style is based on the device appearance.
 * If the device is using Dark mode, the bar text will be light.
 * If the device is using Light mode, the bar text will be dark.
 * On Android the default will be the one the app was launched with.
 * "DEFAULT"
 */

export type $BAR_STYLE =
  | "DARK" // Light text for dark backgrounds.
  | "LIGHT" // Dark text for light backgrounds.
  | "DEFAULT"; // The style is based on the device appearance.

export interface $BarState extends $InsetsState {
  color: string;
  style: $BAR_STYLE;
  visible: boolean;
}

export interface $BarStateColorRGB extends $InsetsState {
  color: $ColorRGB;
  style: $BAR_STYLE;
  visible: boolean;
}

export interface $SafeAreaState {
  overlay: boolean;
  insets: $Insets;
  cutoutInsets: $Insets;
  outerInsets: $Insets;
}

export interface $OverlayState {
  overlay: boolean;
}

export interface $VirtualKeyboardState extends $InsetsState {
  visible: boolean;
}

export interface $Insets {
  top: number;
  right: number;
  bottom: number;
  left: number;
}

export interface $InsetsState {
  overlay: boolean;
  insets: $Insets;
}

export interface $ShareOptions {
  title: string;
  text: string;
  link: string;
  src: string;
  body: Uint8Array;
  bodyType: string;
}

export interface $ColorRGB {
  red: number;
  green: number;
  blue: number;
  alpha: number;
}

export type $ToastPosition = "top" | "bottom";
export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";

export interface $AllWebviewState {
  [key: number]: $WebViewState;
}

export interface $WebViewState {
  statusBarState: $BarState;
  navigationBarState: $BarState;
  safeAreaState: $SafeAreaState;
  webviewId: number;
  isActivated: boolean;
}
export const enum MWEBVIEW_LIFECYCLE_EVENT {
  State = "state", // 获取窗口状态
  Activity = "activity", // 激活应用程序时发出。各种操作都可以触发此事件，例如首次启动应用程序、在应用程序已运行时尝试重新启动该应用程序，或者单击应用程序的停靠栏或任务栏图标。
  Close = "close", // 关闭app
  WindowAllClosed = "window-all-closed", // 关闭应用程序窗口
  DidBecomeActive = "did-become-active", // 每次应用程序激活时都会发出，而不仅仅是在单击 Dock 图标或重新启动应用程序时发出。
  Quit = "quit", // 尝试关闭所有窗口。该before-quit事件将首先发出。如果所有窗口都成功关闭，will-quit将发出该事件，默认情况下应用程序将终止。当前只有quit事件。
  BeforeQuit = "before-quit",
  willQuit = "will-quit",
  Exit = "exit", // 所有窗口将在不询问用户的情况下立即关闭，并且不会发出before-quit 和事件。will-quit
  Relaunch = "relaunch", // 当前实例退出时重新启动应用程序。
}

export const enum MWEBVIEW_OBSERVE {
  State = "observe",
  UpdateProgress = "observeUpdateProgress",
}
