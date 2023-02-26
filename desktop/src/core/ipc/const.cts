import type { Ipc } from "./ipc.cjs";
import type { IpcReqMessage, IpcRequest } from "./IpcRequest.cjs";
import type { IpcResMessage, IpcResponse } from "./IpcResponse.cjs";
import type { IpcStreamAbort } from "./IpcStreamAbort.cjs";
import type { IpcStreamData } from "./IpcStreamData.cjs";
import type { IpcStreamEnd } from "./IpcStreamEnd.cjs";
import type { IpcStreamPull } from "./IpcStreamPull.cjs";

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

export const enum IPC_DATA_TYPE {
  // /** 特殊位：结束符 */
  // END = 1,
  /** 类型：请求 */
  REQUEST,
  /** 类型：相应 */
  RESPONSE,
  /** 类型：流数据，发送方 */
  STREAM_DATA,
  /** 类型：流拉取，请求方 */
  STREAM_PULL,
  /** 类型：流关闭，发送方
   * 可能是发送完成了，也有可能是被中断了
   */
  STREAM_END,
  /** 类型：流中断，请求方 */
  STREAM_ABORT,
}

export type $MetaBody = Readonly<
  | [typeof IPC_RAW_BODY_TYPE.TEXT, string]
  | [typeof IPC_RAW_BODY_TYPE.BASE64, string]
  | [typeof IPC_RAW_BODY_TYPE.TEXT_STREAM_ID, string]
  | [typeof IPC_RAW_BODY_TYPE.BASE64_STREAM_ID, string]
  | [typeof IPC_RAW_BODY_TYPE.BINARY, Uint8Array]
  | [typeof IPC_RAW_BODY_TYPE.BINARY_STREAM_ID, string]
>;
export enum IPC_RAW_BODY_TYPE {
  /** 文本 json html 等 */
  TEXT = 1 << 1,
  /** 使用文本表示的二进制 */
  BASE64 = 1 << 2,
  /** 二进制 */
  BINARY = 1 << 3,
  /** 流 */
  STREAM_ID = 1 << 4,
  /** 文本流 */
  TEXT_STREAM_ID = IPC_RAW_BODY_TYPE.STREAM_ID | IPC_RAW_BODY_TYPE.TEXT,
  /** 文本二进制流 */
  BASE64_STREAM_ID = IPC_RAW_BODY_TYPE.STREAM_ID | IPC_RAW_BODY_TYPE.BASE64,
  /** 二进制流 */
  BINARY_STREAM_ID = IPC_RAW_BODY_TYPE.STREAM_ID | IPC_RAW_BODY_TYPE.BINARY,
}

export const enum IPC_ROLE {
  SERVER = "server",
  CLIENT = "client",
}

export class IpcMessage<T extends IPC_DATA_TYPE> {
  constructor(readonly type: T) {}
}

/** 接收到的消息，可传输的数据 */
export type $IpcTransferableMessage =
  | IpcReqMessage
  | IpcResMessage
  | IpcStreamData
  | IpcStreamPull
  | IpcStreamEnd
  | IpcStreamAbort;

/** 发送的消息 */
export type $IpcMessage =
  | IpcRequest
  | IpcResponse
  | IpcStreamData
  | IpcStreamPull
  | IpcStreamEnd
  | IpcStreamAbort;

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
