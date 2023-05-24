import { Ipc, IpcRequest } from "../../../../core/ipc/index.js";
import type { $Schema1ToType } from "../../../../helper/types.js";
import type { ShareNMM } from "./share.main.js";
/**
 * 设置状态
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
export declare function share(this: ShareNMM, args: $Schema1ToType<{}>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<Response>;
export declare function createStreamIpc(this: ShareNMM, args: $Schema1ToType<{}>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<ReadableStream<Uint8Array>>;
