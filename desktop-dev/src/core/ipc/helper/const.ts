import { simpleDecoder, simpleEncoder } from "../../../helper/encoding.ts";
import { $MicroModuleManifest } from "../../types.ts";
import type { IpcError } from "../IpcError.ts";
import type { IpcEvent } from "../IpcEvent.ts";
import type { IpcLifeCycle } from "../IpcLifeCycle.ts";
import type { IpcReqMessage, IpcRequest } from "../IpcRequest.ts";
import type { IpcResMessage, IpcResponse } from "../IpcResponse.ts";
import type { Ipc } from "../ipc.ts";
import type { IpcStreamAbort } from "../stream/IpcStreamAbort.ts";
import type { IpcStreamData } from "../stream/IpcStreamData.ts";
import type { IpcStreamEnd } from "../stream/IpcStreamEnd.ts";
import type { IpcStreamPaused } from "../stream/IpcStreamPaused.ts";
import type { IpcStreamPulling } from "../stream/IpcStreamPulling.ts";
import type { IpcPoolPack } from "./IpcMessage.ts";

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
  /** 类型：流拉取，请求方
   * 发送方一旦收到该指令，就可以持续发送数据
   * 该指令中可以携带一些“限流协议信息”，如果违背该协议，请求方可能会断开连接
   */
  STREAM_PULLING,
  /** 类型：流暂停，请求方
   * 发送方一旦收到该指令，就应当停止基本的数据发送
   * 该指令中可以携带一些“保险协议信息”，描述仍然允许发送的一些数据类型、发送频率等。如果违背该协议，请求方可以会断开连接
   */
  STREAM_PAUSED,
  /** 类型：流关闭，发送方
   * 可能是发送完成了，也有可能是被中断了
   */
  STREAM_END,
  /** 类型：流中断，请求方 */
  STREAM_ABORT,
  /** 类型：事件 */
  EVENT,
  /**错误响应 */
  ERROR,
  /**生命周期 */
  LIFE_CYCLE,
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

export const enum IPC_STATE {
  OPENING = 1,
  OPEN = 2,
  CLOSING = 3,
  CLOSED = 4,
}
/** 接收到的消息，可传输的数据 */
export type $IpcTransferableMessage =
  | IpcReqMessage
  | IpcResMessage
  | IpcEvent
  | $IpcStreamMessage
  | IpcError
  | IpcLifeCycle;

/** 发送的消息 */
export type $IpcMessage = IpcRequest | IpcResponse | IpcEvent | $IpcStreamMessage | IpcError | IpcLifeCycle;

export type $IpcStreamMessage = IpcStreamData | IpcStreamPulling | IpcStreamPaused | IpcStreamEnd | IpcStreamAbort;

export type $IpcOptions = {
  /**远程的模块是谁*/
  remote: $MicroModuleManifest;
  /**当endpoint为Worker的时候需要传递*/
  port?: MessagePort;
  /**当endpoint为Kotlin的时候需要传递*/
  channel?: MessagePort;
  /**当endpoint为FrontEnd的时候需要传递*/
  stream?: ReadableStream;
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
