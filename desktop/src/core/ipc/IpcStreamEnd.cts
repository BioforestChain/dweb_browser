import { IpcMessage, IPC_DATA_TYPE } from "./const.cjs";

export class IpcStreamEnd extends IpcMessage<IPC_DATA_TYPE.STREAM_END> {
  constructor(readonly stream_id: string) {
    super(IPC_DATA_TYPE.STREAM_END);
  }
}
