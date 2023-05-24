import type { Ipc, IpcRequest } from "../../../../core/ipc/index.js";
import type { $Schema1ToType } from "../../../../helper/types.js";
import type { ToastNMM } from "./toast.main.js";
/**
 * 设置状态
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
export declare function show(this: ToastNMM, args: $Schema1ToType<{}>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<Response>;
