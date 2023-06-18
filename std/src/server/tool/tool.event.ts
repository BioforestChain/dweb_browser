export enum EVENT {
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

export enum OBSERVE {
  State = "observe",
}

export type WebViewState = {
  isActivated: boolean;
  webviewId: string;
};

export interface  AllWebviewState{
  [key: number]: WebViewState
}

