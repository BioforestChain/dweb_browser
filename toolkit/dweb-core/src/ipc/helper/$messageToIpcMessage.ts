import * as CBOR from "cbor-x";
import { ENDPOINT_MESSAGE_TYPE, type $EndpointMessage, type $EndpointRawMessage } from "../endpoint/EndpointMessage.ts";
import type { $IpcMessage, $IpcRawMessage } from "../ipc-message/IpcMessage.ts";
import { IpcClientRequest } from "../ipc-message/IpcRequest.ts";
import { IpcResponse } from "../ipc-message/IpcResponse.ts";
import { IPC_MESSAGE_TYPE } from "../ipc-message/internal/IpcMessage.ts";
import { IpcBodyReceiver } from "../ipc-message/stream/IpcBodyReceiver.ts";
import { MetaBody } from "../ipc-message/stream/MetaBody.ts";
import type { Ipc } from "../ipc.ts";
import { IpcHeaders } from "./IpcHeaders.ts";

export type $JSON<T> = {
  [key in keyof T]: T[key] extends Function ? never : T[key];
};

// export type $IpcSignalMessage = "close" | "ping" | "pong";
// export const $isIpcSignalMessage = (msg: unknown): msg is $IpcSignalMessage =>
//   msg === "close" || msg === "ping" || msg === "pong";

export const $endpointMessageToCbor = (message: $EndpointMessage) => CBOR.encode($serializableEndpointMessage(message));
export const $endpointMessageToJson = (message: $EndpointMessage) =>
  JSON.stringify($serializableEndpointMessage(message));

export const $cborToEndpointMessage = (data: Uint8Array | ArrayBuffer | SharedArrayBuffer) => {
  return CBOR.decode(ArrayBuffer.isView(data) ? data : new Uint8Array(data)) as $EndpointRawMessage;
};

export const $jsonToEndpointMessage = (data: string) => JSON.parse(data) as $EndpointRawMessage;

export const $normalizeIpcMessage = (ipcMessage: $IpcRawMessage, ipc: Ipc): $IpcMessage => {
  switch (ipcMessage.type) {
    case IPC_MESSAGE_TYPE.REQUEST: {
      return new IpcClientRequest(
        ipcMessage.reqId,
        ipcMessage.url,
        ipcMessage.method,
        new IpcHeaders(ipcMessage.headers),
        IpcBodyReceiver.from(MetaBody.fromJSON(ipcMessage.metaBody), ipc),
        ipc
      );
    }
    case IPC_MESSAGE_TYPE.RESPONSE: {
      return new IpcResponse(
        ipcMessage.reqId,
        ipcMessage.statusCode,
        new IpcHeaders(ipcMessage.headers),
        IpcBodyReceiver.from(MetaBody.fromJSON(ipcMessage.metaBody), ipc),
        ipc
      );
    }
    default:
      return ipcMessage;
  }
};
export const $serializableEndpointMessage = (message: $EndpointMessage): $EndpointRawMessage => {
  switch (message.type) {
    case ENDPOINT_MESSAGE_TYPE.LIFECYCLE:
      return message;
    case ENDPOINT_MESSAGE_TYPE.IPC:
      switch (message.ipcMessage.type) {
        case IPC_MESSAGE_TYPE.REQUEST:
          return {
            ...message,
            ipcMessage: message.ipcMessage.toSerializable(),
          };
        case IPC_MESSAGE_TYPE.RESPONSE:
          return {
            ...message,
            ipcMessage: message.ipcMessage.toSerializable(),
          };
        default:
          return message as $EndpointRawMessage;
      }
  }
};
// /**
//  * 内存传输转换为message
//  * @param data
//  * @param ipc
//  * @returns
//  */
// export const $messageToIpcMessage = (data: $JSON<IpcPoolPack>, ipc: Ipc) => {
//   const ipcMessage = $normalizeIpcMessage(data.ipcMessage as $JSON<$IpcTransferableMessage>, ipc);
//   return new IpcPoolPack(data.pid, ipcMessage);
// };

// /**
//  * 把字符串转换为message
//  * @param data
//  * @param ipc
//  * @returns IpcPoolPack
//  */
// export const $jsonToIpcMessage = (data: string, ipc: Ipc) => {
//   const pack = JSON.parse(data) as $JSON<IpcPoolPackString>;
//   const ipcMessage = $normalizeIpcMessage(JSON.parse(pack.ipcMessage), ipc);
//   return new IpcPoolPack(pack.pid, ipcMessage);
// };
// /**
//  * 将字节转换成message
//  * @param data
//  * @param ipc
//  * @returns IpcPoolPack
//  */
// export const $cborToIpcMessage = (data: Uint8Array, ipc: Ipc) => {
//   const pack = decode(data) as $JSON<PackIpcMessage>;
//   const ipcMessage = $normalizeIpcMessage(decode(pack.messageByteArray) as $JSON<$IpcTransferableMessage>, ipc);
//   return new IpcPoolPack(pack.pid, ipcMessage);
// };

// const textDecoder = new TextDecoder();
// export const $uint8ArrayToIpcMessage = (data: Uint8Array, ipc: Ipc) => {
//   return $jsonToIpcMessage(textDecoder.decode(data), ipc);
// };
