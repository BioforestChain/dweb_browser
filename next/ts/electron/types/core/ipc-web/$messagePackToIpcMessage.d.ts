import type { Ipc } from "../ipc/ipc.js";
import { $IpcSignalMessage } from "./$messageToIpcMessage.js";
export declare const $messagePackToIpcMessage: (data: Uint8Array, ipc: Ipc) => import("../ipc/const.js").$IpcMessage | $IpcSignalMessage | undefined;
