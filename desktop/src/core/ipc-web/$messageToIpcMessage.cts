import {
  $IpcMessage,
  $IpcTransferableMessage,
  IPC_MESSAGE_TYPE,
} from "../ipc/const.cjs";
import type { Ipc } from "../ipc/ipc.cjs";
import { IpcBodyReceiver } from "../ipc/IpcBodyReceiver.cjs";
import { IpcEvent } from "../ipc/IpcEvent.cjs";
import { IpcHeaders } from "../ipc/IpcHeaders.cjs";
import { IpcRequest } from "../ipc/IpcRequest.cjs";
import { IpcResponse } from "../ipc/IpcResponse.cjs";
import { IpcStreamData } from "../ipc/IpcStreamData.cjs";
import { IpcStreamEnd } from "../ipc/IpcStreamEnd.cjs";
import { IpcStreamPaused } from "../ipc/IpcStreamPaused.cjs";
import { IpcStreamPulling } from "../ipc/IpcStreamPulling.cjs";
import { MetaBody } from "../ipc/MetaBody.cjs";

export type $JSON<T> = {
  [key in keyof T]: T[key] extends Function ? never : T[key];
};

export type $IpcSignalMessage = "close" | "ping" | "pong";
export const $isIpcSignalMessage = (msg: unknown): msg is $IpcSignalMessage =>
  msg === "close" || msg === "ping" || msg === "pong";
export const $objectToIpcMessage = (
  data: $JSON<$IpcTransferableMessage>,
  ipc: Ipc
) => {
  let message: undefined | $IpcMessage | $IpcSignalMessage;

  if (data.type === IPC_MESSAGE_TYPE.REQUEST) {
    message = new IpcRequest(
      data.req_id,
      data.url,
      data.method,
      new IpcHeaders(data.headers),
      new IpcBodyReceiver(MetaBody.fromJSON(data.metaBody), ipc),
      ipc
    );
  } else if (data.type === IPC_MESSAGE_TYPE.RESPONSE) {
    message = new IpcResponse(
      data.req_id,
      data.statusCode,
      new IpcHeaders(data.headers),
      new IpcBodyReceiver(MetaBody.fromJSON(data.metaBody), ipc),
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
  } else if (data.type === IPC_MESSAGE_TYPE.STREAM_END) {
    message = new IpcStreamEnd(data.stream_id);
  }
  return message;
};

export const $messageToIpcMessage = (
  data: $JSON<$IpcTransferableMessage> | $IpcSignalMessage,
  ipc: Ipc
) => {
  if ($isIpcSignalMessage(data)) {
    return data;
  }

  return $objectToIpcMessage(data, ipc);
};
export const $jsonToIpcMessage = (data: string, ipc: Ipc) => {
  if ($isIpcSignalMessage(data)) {
    return data;
  }

  return $objectToIpcMessage(JSON.parse(data), ipc);
};
