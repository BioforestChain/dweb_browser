import { IpcMessage, IPC_MESSAGE_TYPE } from "./const.cjs";

export class IpcStreamPull extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_PULL> {
  readonly desiredSize: number;
  constructor(readonly stream_id: string, desiredSize?: number | null) {
    super(IPC_MESSAGE_TYPE.STREAM_PULL);
    if (desiredSize == null) {
      desiredSize = 1;
    } else if (Number.isFinite(desiredSize) === false) {
      desiredSize = 1;
    } else if (desiredSize < 1) {
      desiredSize = 1;
    }
    this.desiredSize = desiredSize;
  }
}
