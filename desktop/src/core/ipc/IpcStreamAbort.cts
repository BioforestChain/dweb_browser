import { IpcMessage, IPC_DATA_TYPE } from "./const.cjs";

export class IpcStreamAbort extends IpcMessage<IPC_DATA_TYPE.STREAM_ABORT> {
  constructor(readonly stream_id: string) {
    super(IPC_DATA_TYPE.STREAM_ABORT);
  }
}
