import { decode } from "@msgpack/msgpack";
import type { $IpcTransferableMessage, IpcMessage } from "../ipc/const.ts";
import type { Ipc } from "../ipc/ipc.ts";
import {
  $IpcSignalMessage,
  $JSON,
  $messageToIpcMessage,
} from "./$messageToIpcMessage.ts";

export const $messagePackToIpcMessage = (data: Uint8Array, ipc: Ipc) => {
  return $messageToIpcMessage(
    decode(data) as $JSON<$IpcTransferableMessage> | $IpcSignalMessage,
    ipc
  );
};
