import { simpleDecoder, simpleEncoder } from "../../../helper/encoding.ts";
import { $MicroModuleManifest } from "../../types.ts";
import type { IpcError } from "../ipc-message/IpcError.ts";
import type { IpcEvent } from "../ipc-message/IpcEvent.ts";
import { IpcFork } from "../ipc-message/IpcFork.ts";
import type { IpcLifeCycle } from "../ipc-message/IpcLifeCycle.ts";
import type { IpcPoolPack } from "../ipc-message/IpcMessage.ts";
import type { IpcReqMessage, IpcRequest } from "../ipc-message/IpcRequest.ts";
import type { IpcResMessage, IpcResponse } from "../ipc-message/IpcResponse.ts";
import type { Ipc } from "../ipc.ts";
import type { IpcStreamAbort } from "../stream/IpcStreamAbort.ts";
import type { IpcStreamData } from "../stream/IpcStreamData.ts";
import type { IpcStreamEnd } from "../stream/IpcStreamEnd.ts";
import type { IpcStreamPaused } from "../stream/IpcStreamPaused.ts";
import type { IpcStreamPulling } from "../stream/IpcStreamPulling.ts";

export const enum IPC_METHOD {
  GET = "GET",
  POST = "POST",
  PUT = "PUT",
  DELETE = "DELETE",
  OPTIONS = "OPTIONS",
  TRACE = "TRACE",
  PATCH = "PATCH",
  PURGE = "PURGE",
  HEAD = "HEAD",
}
export const toIpcMethod = (method?: string) => {
  if (method == null) {
    return IPC_METHOD.GET;
  }

  switch (method.toUpperCase()) {
    case IPC_METHOD.GET: {
      return IPC_METHOD.GET;
    }
    case IPC_METHOD.POST: {
      return IPC_METHOD.POST;
    }
    case IPC_METHOD.PUT: {
      return IPC_METHOD.PUT;
    }
    case IPC_METHOD.DELETE: {
      return IPC_METHOD.DELETE;
    }
    case IPC_METHOD.OPTIONS: {
      return IPC_METHOD.OPTIONS;
    }
    case IPC_METHOD.TRACE: {
      return IPC_METHOD.TRACE;
    }
    case IPC_METHOD.PATCH: {
      return IPC_METHOD.PATCH;
    }
    case IPC_METHOD.PURGE: {
      return IPC_METHOD.PURGE;
    }
    case IPC_METHOD.HEAD: {
      return IPC_METHOD.HEAD;
    }
  }
  throw new Error(`invalid method: ${method}`);
};

/** 接收到的消息，可传输的数据 */
export type $IpcTransferableMessage =
  | IpcReqMessage
  | IpcResMessage
  | IpcEvent
  | $IpcStreamMessage
  | IpcError
  | IpcLifeCycle;

/** 发送的消息 */
export type $IpcMessage = IpcRequest | IpcResponse | IpcEvent | $IpcStreamMessage | IpcError | IpcLifeCycle | IpcFork;

export type $IpcStreamMessage = IpcStreamData | IpcStreamPulling | IpcStreamPaused | IpcStreamEnd | IpcStreamAbort;

export type $IpcOptions = {
  /**是否自动启动，可以在这个启动之前做一些动作默认不传递为自动启动 */
  autoStart?: false;
  /**远程的模块是谁*/
  remote: $MicroModuleManifest;
  /**当endpoint为Worker的时候需要传递*/
  port?: MessagePort;
  /**当endpoint为Kotlin的时候需要传递*/
  channel?: MessagePort;
};

/**IpcPool*/
export type $OnIpcPool = (
  /// 这里只会有两种类型的数据
  message: IpcPoolPack,
  ipc: Ipc
) => unknown;

export type $OnIpcMessage = (
  /// 这里只会有两种类型的数据
  message: $IpcMessage,
  ipc: Ipc
) => unknown;

export type $OnIpcRequestMessage = (
  /// 这里只会有两种类型的数据
  message: IpcRequest,
  ipc: Ipc
) => unknown;
export type $OnIpcEventMessage = (
  /// 这里只会有两种类型的数据
  message: IpcEvent,
  ipc: Ipc
) => unknown;
export type $OnIpcStreamMessage = (
  /// 这里只会有两种类型的数据
  message: $IpcStreamMessage,
  ipc: Ipc
) => unknown;

export type $OnIpcLifeCycleMessage = (
  /// 这里只会有两种类型的数据
  message: IpcLifeCycle,
  ipc: Ipc
) => unknown;

export type $OnIpcErrorMessage = (
  /// 这里只会有两种类型的数据
  message: IpcError,
  ipc: Ipc
) => unknown;

export const $dataToBinary = (data: string | Uint8Array, encoding: IPC_DATA_ENCODING) => {
  switch (encoding) {
    case IPC_DATA_ENCODING.BINARY: {
      return data as Uint8Array;
    }
    case IPC_DATA_ENCODING.BASE64: {
      return simpleEncoder(data as string, "base64");
    }
    case IPC_DATA_ENCODING.UTF8: {
      return simpleEncoder(data as string, "utf8");
    }
  }
  throw new Error(`unknown encoding: ${encoding}`);
};
export const $dataToText = (data: string | Uint8Array, encoding: IPC_DATA_ENCODING) => {
  switch (encoding) {
    case IPC_DATA_ENCODING.BINARY: {
      return simpleDecoder(data as Uint8Array, "utf8");
    }
    case IPC_DATA_ENCODING.BASE64: {
      return simpleDecoder(simpleEncoder(data as string, "base64"), "utf8");
    }
    case IPC_DATA_ENCODING.UTF8: {
      return data as string;
    }
  }
  throw new Error(`unknown encoding: ${encoding}`);
};
