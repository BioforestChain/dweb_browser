export type $MMID = `${string}.dweb`;
export type $DWEB_DEEPLINK = `dweb:${string}`;
/**
 * 通讯支持的传输协议
 */
export interface $IpcSupportProtocols {
  cbor: boolean;
  protobuf: boolean;
  raw: boolean;
}
export interface $IpcMicroModuleInfo {
  /** 模块id */
  readonly mmid: $MMID;
  /** 对通讯协议的支持情况 */
  readonly ipc_support_protocols: $IpcSupportProtocols;
  /**
   * 匹配的“DWEB深层链接”
   * 取代明确的 mmid，dweb-deeplinks 可以用来表征一种特性、一种共识，它必须是 'dweb:{domain}[/pathname[/pathname...]]' 的格式规范
   * 为了交付给用户清晰的可管理的模式，这里的 deeplink 仅仅允许精确的前缀匹配，因此我们通常会规范它的动作层级依次精确
   *
   * 比如说：'dweb:mailto'，那么在面对 'dweb:mailto?address=someone@mail.com&title=xxx' 的链接时，该链接会被打包成一个 IpcRequest 消息传输过来
   * 比如说：'dweb:open/file/image'，那么就会匹配这样的链接 'dweb:open/file/image/svg?uri=file:///example.svg'
   *
   * dweb_deeplinks 由 dns 模块进行统一管理，也由它提供相关的管理界面、控制策略
   */
  readonly dweb_deeplinks: $DWEB_DEEPLINK[];
}
export interface $MicroModule extends $IpcMicroModuleInfo {
  nativeFetch(
    input: RequestInfo | URL,
    init?: RequestInit
  ): Promise<Response> &
    typeof import("../helper/fetchExtends/index.ts")["fetchExtends"];

  /**
   * 添加双工连接到自己的池子中，但自己销毁，这些双工连接都会被断掉
   * @param ipc
   */
  addToIpcSet(ipc: import("./ipc/ipc.ts").Ipc): void;
}

export const enum MWEBVIEW_LIFECYCLE_EVENT {
  // State = "state", // 获取窗口状态
  Activity = "activity", // 激活应用程序时发出。各种操作都可以触发此事件，例如首次启动应用程序、在应用程序已运行时尝试重新启动该应用程序，或者单击应用程序的停靠栏或任务栏图标。
  Close = "close", // 关闭app
  // WindowAllClosed = "window-all-closed", // 关闭应用程序窗口
  // DidBecomeActive = "did-become-active", // 每次应用程序激活时都会发出，而不仅仅是在单击 Dock 图标或重新启动应用程序时发出。
  // Quit = "quit", // 尝试关闭所有窗口。该before-quit事件将首先发出。如果所有窗口都成功关闭，will-quit将发出该事件，默认情况下应用程序将终止。当前只有quit事件。
  // BeforeQuit = "before-quit",
  // willQuit = "will-quit",
  // Exit = "exit", // 所有窗口将在不询问用户的情况下立即关闭，并且不会发出before-quit 和事件。will-quit
  // Relaunch = "relaunch", // 当前实例退出时重新启动应用程序。
}
