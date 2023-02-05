import { IPC_DATA_TYPE } from "./const.cjs";

export class IpcStreamPull {
  readonly type = IPC_DATA_TYPE.STREAM_PULL;
  readonly desiredSize: number;
  constructor(readonly stream_id: string, desiredSize?: number | null) {
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
