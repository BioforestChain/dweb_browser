import { decode } from "cbor-x";
import type { Ipc } from "../ipc.ts";
import { $JSON, $messageToIpcMessage } from "./$messageToIpcMessage.ts";
import type { $IpcTransferableMessage } from "./const.ts";

export const $messagePackToIpcMessage = (data: Uint8Array, ipc: Ipc) => {
  return $messageToIpcMessage(decode(data) as $JSON<$IpcTransferableMessage>, ipc); // | $IpcSignalMessage
};
