import { decode } from "@msgpack/msgpack";
import type { $IpcTransferableMessage, IpcMessage } from "../ipc/const.cjs";
import type { Ipc } from "../ipc/ipc.cjs";
import {
  $IpcSignalMessage,
  $JSON,
  $messageToIpcMessage,
} from "./$messageToIpcMessage.cjs";

export const $messagePackToIpcMessage = (data: Uint8Array, ipc: Ipc) => {
  return $messageToIpcMessage(
    decode(data) as $JSON<$IpcTransferableMessage> | $IpcSignalMessage,
    ipc
  );
};
