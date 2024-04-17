import { $IpcError } from "./IpcError.ts";
import { $IpcEvent } from "./IpcEvent.ts";
import { $IpcFork } from "./IpcFork.ts";
import { $IpcLifecycle } from "./IpcLifecycle.ts";
import { $IpcRequest, IpcClientRequest, IpcServerRequest } from "./IpcRequest.ts";
import { $IpcResponse, IpcResponse } from "./IpcResponse.ts";
import { $IpcStream } from "./stream/IpcStream.ts";

/** 发送的消息 */
export type $IpcRawMessage = $IpcRequest | $IpcResponse | $IpcEvent | $IpcStream | $IpcError | $IpcLifecycle | $IpcFork;
export type $IpcMessage =
  | IpcClientRequest
  | IpcServerRequest
  | IpcResponse
  | $IpcEvent
  | $IpcStream
  | $IpcError
  | $IpcLifecycle
  | $IpcFork;
