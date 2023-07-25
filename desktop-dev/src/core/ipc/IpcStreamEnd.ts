import { IPC_MESSAGE_TYPE, IpcMessage } from "./const.ts";

export class IpcStreamEnd extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_END> {
  constructor(readonly stream_id: string) {
    super(IPC_MESSAGE_TYPE.STREAM_END);
  }
}
