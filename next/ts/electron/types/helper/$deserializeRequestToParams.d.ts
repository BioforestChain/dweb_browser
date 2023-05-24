import type { IpcRequest } from "../core/ipc/index.js";
import type { $Schema1, $Schema1ToType } from "./types.js";
export declare const $deserializeRequestToParams: <S extends $Schema1>(schema: S) => (request: IpcRequest) => $Schema1ToType<S>;
