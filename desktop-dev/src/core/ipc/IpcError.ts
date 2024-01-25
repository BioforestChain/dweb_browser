import { IPC_MESSAGE_TYPE, IpcMessage } from "./const.ts";

export class IpcError extends IpcMessage<IPC_MESSAGE_TYPE.ERROR> {
  constructor(readonly errorCode: number, readonly message?: string) {
    super(IPC_MESSAGE_TYPE.ERROR);
  }
}
