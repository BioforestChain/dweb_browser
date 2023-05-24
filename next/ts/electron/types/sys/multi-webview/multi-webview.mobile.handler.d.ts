import type { Ipc } from "../../core/ipc/index.js";
import type { IpcRequest } from "../../core/ipc/IpcRequest.js";
import type { $Schema1ToType } from "../../helper/types.js";
import type { MultiWebviewNMM } from "./multi-webview.mobile.js";
type $APIS = typeof import("./assets/multi-webview.html.js")["APIS"];
/**
* 打开 应用
* 如果 是由 jsProcdss 调用 会在当前的 browserWindow 打开一个新的 webview
* 如果 是由 NMM 调用的 会打开一个新的 borserWindow 同时打开一个新的 webview
*/
export declare function open(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{
    url: "string";
}>, clientIpc: Ipc, request: IpcRequest): Promise<any>;
/**
 * 关闭当前激活项
 * @param this
 * @param root_url
 * @param args
 * @param clientIpc
 * @param request
 * @returns
 */
export declare function closeFocusedWindow(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<boolean>;
export declare function openDownloadPage(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{
    url: "string";
}>, clientIpc: Ipc, request: IpcRequest): Promise<{}>;
/**
 * 设置状态栏
 * @param this
 * @param root_url
 * @param args
 * @param clientIpc
 * @param request
 * @returns
 */
export declare function barGetState<$ApiKeyName extends keyof Pick<$APIS, "statusBarGetState" | "navigationBarGetState">>(this: MultiWebviewNMM, apiksKeyName: $ApiKeyName, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<any>;
/**
 * 设置状态
 * @param this
 * @param root_url
 * @param args
 * @param clientIpc
 * @param request
 * @returns
 */
export declare function barSetState<$ApiKeyName extends keyof Pick<$APIS, "statusBarSetState" | "navigationBarSetState">>(this: MultiWebviewNMM, apiKeyName: $ApiKeyName, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<{
    color: import("../plugins/helper.js").$AgbaColor;
    style: import("./types.js").$BAR_STYLE;
    visible: boolean;
    overlay: boolean;
    insets: import("./types.js").$Insets;
} | undefined>;
export declare function safeAreaGetState(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<any>;
export declare function safeAreaSetState(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<any>;
export declare function virtualKeyboardGetState(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<any>;
export declare function virtualKeyboardSetState(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<any>;
export declare function toastShow(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<boolean>;
export declare function shareShare(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<boolean>;
export declare function toggleTorch(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<any>;
export declare function torchState(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<any>;
export declare function haptics(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{
    action: "string";
}>, clientIpc: Ipc, request: IpcRequest): Promise<any>;
export declare function biometricsMock(this: MultiWebviewNMM, root_url: string, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<boolean>;
export {};
