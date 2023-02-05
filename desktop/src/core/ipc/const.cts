import type { Ipc } from "./ipc.cjs";
import type { IpcRequest } from "./IpcRequest.cjs";
import type { IpcResponse } from "./IpcResponse.cjs";
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
  /** 类型：流数据 */
  STREAM_DATA,
  /** 类型：流拉取 */
  STREAM_PULL,
  /** 类型：流关闭 */
  STREAM_END,
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
  | IpcStreamEnd;
export type $IpcOnMessage = (message: $IpcMessage, ipc: Ipc) => unknown;
