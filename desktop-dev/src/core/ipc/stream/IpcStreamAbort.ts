import { IPC_MESSAGE_TYPE } from "../helper/const.ts";
import { IpcMessage } from "../helper/IpcMessage.ts";

export class IpcStreamAbort extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_ABORT> {
  constructor(readonly stream_id: string) {
    super(IPC_MESSAGE_TYPE.STREAM_ABORT);
  }
}
