import type { Ipc, IpcRequest } from "../../../../core/ipc/index.js";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.js";
import type { StatusbarNativeUiNMM } from "./status-bar.main.js";
/**
 * 获取状态
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
export declare function getState(this: StatusbarNativeUiNMM, args: $Schema1ToType<$Schema1>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<Response>;
/**
 * 设置状态
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
export declare function setState(this: StatusbarNativeUiNMM, args: $Schema1ToType<{}>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<Response>;
/**
 * 开启监听
 * @param this
 * @param args
 * @param clientIpc
 * @param request
 * @returns
 */
export declare function startObserve(this: StatusbarNativeUiNMM, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<boolean>;
/**
 * 停止 监听
 * @param this
 * @param args
 * @param clientIpc
 * @param request
 * @returns
 */
export declare function stopObserve(this: StatusbarNativeUiNMM, args: $Schema1ToType<{}>, clientIpc: Ipc, request: IpcRequest): Promise<boolean>;
