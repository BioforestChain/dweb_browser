import { IPC_MESSAGE_TYPE, IpcMessage } from "./const.ts";

export class IpcStreamAbort extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_ABORT> {
  constructor(readonly stream_id: string) {
    super(IPC_MESSAGE_TYPE.STREAM_ABORT);
  }
}
