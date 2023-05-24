import { Ipc, IpcRequest, IpcResponse } from "../core/ipc/index.js";
import type { $Schema2, $Schema2ToType } from "./types.js";
/**
 * @TODO 实现模式匹配
 */
export declare const $serializeResultToResponse: <S extends $Schema2>(schema: S) => (request: IpcRequest, result: $Schema2ToType<S>, ipc: Ipc) => IpcResponse | Promise<IpcResponse>;
