import { decode } from "cbor-x";
import { IpcError } from "../ipc-message/IpcError.ts";
import { IpcEvent } from "../ipc-message/IpcEvent.ts";
import { IpcLifeCycle } from "../ipc-message/IpcLifeCycle.ts";
import { IpcRequest } from "../ipc-message/IpcRequest.ts";
import { IpcResponse } from "../ipc-message/IpcResponse.ts";
import { IpcPoolPack, IpcPoolPackString, PackIpcMessage } from "../index.ts";
import type { Ipc } from "../ipc.ts";
import { IpcBodyReceiver } from "../stream/IpcBodyReceiver.ts";
import { IpcStreamAbort } from "../stream/IpcStreamAbort.ts";
import { IpcStreamData } from "../stream/IpcStreamData.ts";
import { IpcStreamEnd } from "../stream/IpcStreamEnd.ts";
import { IpcStreamPaused } from "../stream/IpcStreamPaused.ts";
import { IpcStreamPulling } from "../stream/IpcStreamPulling.ts";
import { MetaBody } from "../stream/MetaBody.ts";
import { IpcHeaders } from "./IpcHeaders.ts";
import { $IpcMessage, $IpcTransferableMessage, IPC_MESSAGE_TYPE } from "./const.ts";

export type $JSON<T> = {
  [key in keyof T]: T[key] extends Function ? never : T[key];
};

// export type $IpcSignalMessage = "close" | "ping" | "pong";
// export const $isIpcSignalMessage = (msg: unknown): msg is $IpcSignalMessage =>
//   msg === "close" || msg === "ping" || msg === "pong";

/**
 * 针对各个IpcMessage进行构造
 * @param data
 * @param ipc
 * @returns $IpcMessage
 */
export const $objectToIpcMessage = (data: $JSON<$IpcTransferableMessage>, ipc: Ipc) => {
  let message: $IpcMessage = IpcError.internalServer("$objectToIpcMessage decode error"); // | $IpcSignalMessage
  if (data.type === IPC_MESSAGE_TYPE.REQUEST) {
    message = new IpcRequest(
      data.reqId,
      data.url,
      data.method,
      new IpcHeaders(data.headers),
      IpcBodyReceiver.from(MetaBody.fromJSON(data.metaBody), ipc),
      ipc
    );
  } else if (data.type === IPC_MESSAGE_TYPE.RESPONSE) {
    message = new IpcResponse(
      data.reqId,
      data.statusCode,
      new IpcHeaders(data.headers),
      IpcBodyReceiver.from(MetaBody.fromJSON(data.metaBody), ipc),
      ipc
    );
  } else if (data.type === IPC_MESSAGE_TYPE.EVENT) {
    message = new IpcEvent(data.name, data.data, data.encoding);
  } else if (data.type === IPC_MESSAGE_TYPE.STREAM_DATA) {
    message = new IpcStreamData(data.stream_id, data.data, data.encoding);
  } else if (data.type === IPC_MESSAGE_TYPE.STREAM_PULLING) {
    message = new IpcStreamPulling(data.stream_id, data.bandwidth);
  } else if (data.type === IPC_MESSAGE_TYPE.STREAM_PAUSED) {
    message = new IpcStreamPaused(data.stream_id, data.fuse);
  } else if (data.type === IPC_MESSAGE_TYPE.STREAM_ABORT) {
    message = new IpcStreamAbort(data.stream_id);
  } else if (data.type === IPC_MESSAGE_TYPE.STREAM_END) {
    message = new IpcStreamEnd(data.stream_id);
  } else if (data.type === IPC_MESSAGE_TYPE.ERROR) {
    message = new IpcError(data.errorCode, data.message);
  } else if (data.type === IPC_MESSAGE_TYPE.LIFE_CYCLE) {
    message = new IpcLifeCycle(data.state, data.encoding);
  }
  return message;
};
/**
 * 内存传输转换为message
 * @param data
 * @param ipc
 * @returns
 */
export const $messageToIpcMessage = (data: $JSON<IpcPoolPack>, ipc: Ipc) => {
  const ipcMessage = $objectToIpcMessage(data.ipcMessage as $JSON<$IpcTransferableMessage>, ipc);
  return new IpcPoolPack(data.pid, ipcMessage);
};

/**
 * 把字符串转换为message
 * @param data
 * @param ipc
 * @returns IpcPoolPack
 */
export const $jsonToIpcMessage = (data: string, ipc: Ipc) => {
  const pack = JSON.parse(data) as $JSON<IpcPoolPackString>;
  const ipcMessage = $objectToIpcMessage(JSON.parse(pack.ipcMessage), ipc);
  return new IpcPoolPack(pack.pid, ipcMessage);
};
/**
 * 将字节转换成message
 * @param data
 * @param ipc
 * @returns IpcPoolPack
 */
export const $cborToIpcMessage = (data: Uint8Array, ipc: Ipc) => {
  const pack = decode(data) as $JSON<PackIpcMessage>;
  const ipcMessage = $objectToIpcMessage(decode(pack.messageByteArray) as $JSON<$IpcTransferableMessage>, ipc);
  return new IpcPoolPack(pack.pid, ipcMessage);
};

const textDecoder = new TextDecoder();
export const $uint8ArrayToIpcMessage = (data: Uint8Array, ipc: Ipc) => {
  return $jsonToIpcMessage(textDecoder.decode(data), ipc);
};
