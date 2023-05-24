import { $IpcMessage, $IpcTransferableMessage } from "../ipc/const.js";
import type { Ipc } from "../ipc/ipc.js";
export type $JSON<T> = {
    [key in keyof T]: T[key] extends Function ? never : T[key];
};
export type $IpcSignalMessage = "close" | "ping" | "pong";
export declare const $isIpcSignalMessage: (msg: unknown) => msg is $IpcSignalMessage;
export declare const $objectToIpcMessage: (data: $JSON<$IpcTransferableMessage>, ipc: Ipc) => $IpcMessage | $IpcSignalMessage | undefined;
export declare const $messageToIpcMessage: (data: $JSON<$IpcTransferableMessage> | $IpcSignalMessage, ipc: Ipc) => $IpcMessage | $IpcSignalMessage | undefined;
export declare const $jsonToIpcMessage: (data: string, ipc: Ipc) => $IpcMessage | $IpcSignalMessage | undefined;
