import { IPC_MESSAGE_TYPE } from "./helper/const.ts";
import { IpcMessage } from "./helper/IpcMessage.ts";

export class IpcError extends IpcMessage<IPC_MESSAGE_TYPE.ERROR> {
  constructor(readonly errorCode: number, readonly message?: string) {
    super(IPC_MESSAGE_TYPE.ERROR);
  }

  static internalServer(message: string) {
    return new IpcError(500, message);
  }
}
