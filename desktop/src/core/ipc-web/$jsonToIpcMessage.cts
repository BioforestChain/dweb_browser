import type { $IpcTransferableMessage, IpcMessage } from "../ipc/const.cjs";
import type { Ipc } from "../ipc/ipc.cjs";
import {
  $IpcSignalMessage,
  $JSON,
  $messageToIpcMessage,
  isIpcSignalMessage,
} from "./$messageToIpcMessage.cjs";

export const $jsonToIpcMessage = (data: string, ipc: Ipc) => {
  return $messageToIpcMessage(
    isIpcSignalMessage(data)
      ? data
      : (JSON.parse(data) as $JSON<$IpcTransferableMessage> | $IpcSignalMessage),
    ipc
  );
};
