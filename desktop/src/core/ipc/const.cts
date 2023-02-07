import type { Ipc } from "./ipc.cjs";
import type { IpcRequest } from "./IpcRequest.cjs";
import type { IpcResponse } from "./IpcResponse.cjs";
import type { IpcStreamAbort } from "./IpcStreamAbort.cjs";
import type { IpcStreamData } from "./IpcStreamData.cjs";
import type { IpcStreamEnd } from "./IpcStreamEnd.cjs";
import type { IpcStreamPull } from "./IpcStreamPull.cjs";

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

export type $RawData = Readonly<
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
