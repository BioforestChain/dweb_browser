import { IpcMessage, IPC_MESSAGE_TYPE } from "./const.cjs";

export class IpcStreamEnd extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_END> {
  constructor(readonly stream_id: string) {
    super(IPC_MESSAGE_TYPE.STREAM_END);
  }
}
