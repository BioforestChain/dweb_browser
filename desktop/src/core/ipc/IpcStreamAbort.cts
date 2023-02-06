import { IPC_DATA_TYPE } from "./const.cjs";

export class IpcStreamAbort {
  readonly type = IPC_DATA_TYPE.STREAM_ABORT;
  constructor(readonly stream_id: string) {}
}
