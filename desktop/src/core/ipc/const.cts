import { simpleDecoder, simpleEncoder } from "../../helper/encoding.cjs";
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

export const enum IPC_MESSAGE_TYPE {
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
  /** 类型：事件 */
  EVENT,
}

/**
 * 数据编码格式
 */
export const enum IPC_DATA_ENCODING {
  /** 文本 json html 等 */
  UTF8 = 1 << 1,
  /** 使用文本表示的二进制 */
  BASE64 = 1 << 2,
  /** 二进制 */
  BINARY = 1 << 3,
}

export const enum IPC_ROLE {
  SERVER = "server",
  CLIENT = "client",
}

export class IpcMessage<T extends IPC_MESSAGE_TYPE> {
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

export const $dataToBinary = (
  data: string | Uint8Array,
  encoding: IPC_DATA_ENCODING
) => {
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
export const $dataToText = (
  data: string | Uint8Array,
  encoding: IPC_DATA_ENCODING
) => {
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
