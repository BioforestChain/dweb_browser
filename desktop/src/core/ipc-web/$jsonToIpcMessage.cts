import type { $IpcTransferableMessage, IpcMessage } from "../ipc/const.cjs";
import type { Ipc } from "../ipc/ipc.cjs";
import {
  $IpcSignalMessage,
  $JSON,
  $messageToIpcMessage,
  isIpcSignalMessage,
} from "./$messageToIpcMessage.cjs";

export const $jsonToIpcMessage = (data: string, ipc: Ipc) => {
  // 原始代码
  // return $messageToIpcMessage(
  //   isIpcSignalMessage(data)
  //     ? data
  //     : (JSON.parse(data) as $JSON<$IpcTransferableMessage> | $IpcSignalMessage),
  //   ipc
  // );

  // 测试代码
  const _data = (Object.prototype.toString.call(data).slice(8, -1) === "Object"
                ? data 
                : JSON.parse(data)) as $JSON<$IpcTransferableMessage> | $IpcSignalMessage
  return $messageToIpcMessage(
    isIpcSignalMessage(data) ? data : _data,
    ipc
  );

};
