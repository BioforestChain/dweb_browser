import { decode } from "@msgpack/msgpack";
import type { $IpcTransferableMessage, IpcMessage } from "../ipc/const.js";
import type { Ipc } from "../ipc/ipc.js";
import {
  $IpcSignalMessage,
  $JSON,
  $messageToIpcMessage,
} from "./$messageToIpcMessage.js";

export const $messagePackToIpcMessage = (data: Uint8Array, ipc: Ipc) => {
  return $messageToIpcMessage(
    decode(data) as $JSON<$IpcTransferableMessage> | $IpcSignalMessage,
    ipc
  );
};
