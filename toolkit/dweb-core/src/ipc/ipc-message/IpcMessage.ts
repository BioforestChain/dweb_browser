import type { $IpcError } from "./IpcError.ts";
import type { $IpcEvent } from "./IpcEvent.ts";
import type { $IpcFork } from "./IpcFork.ts";
import type { $IpcLifecycle } from "./IpcLifecycle.ts";
import type { $IpcRawRequest, IpcClientRequest, IpcServerRequest } from "./IpcRequest.ts";
import type { $IpcRawResponse, IpcResponse } from "./IpcResponse.ts";
import type { $IpcStream } from "./stream/IpcStream.ts";

/** 发送的消息 */
export type $IpcRawMessage = $IpcRawRequest | $IpcRawResponse | $IpcEvent | $IpcStream | $IpcError | $IpcLifecycle | $IpcFork;
export type $IpcMessage =
  | IpcClientRequest
  | IpcServerRequest
  | IpcResponse
  | $IpcEvent
  | $IpcStream
  | $IpcError
  | $IpcLifecycle
  | $IpcFork;
