import type { Ipc, IpcRequest } from "../../core/ipc/index.js";
import type { $Schema1ToType } from "../../helper/types.js";
import type { JmmNMM } from "./jmm.js";
/**
 * 功能：
 * 打开一个新的 webveiw 页面
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
export declare function install(this: JmmNMM, args: $Schema1ToType<{
    metadataUrl: "string";
}>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<boolean>;
export declare function pause(this: JmmNMM, args: $Schema1ToType<{}>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<boolean>;
export declare function resume(this: JmmNMM, args: $Schema1ToType<{}>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<boolean>;
export declare function cancel(this: JmmNMM, args: $Schema1ToType<{}>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<boolean>;
