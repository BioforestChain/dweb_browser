import { IPC_DATA_TYPE } from "./const.cjs";

export class IpcStreamEnd {
  readonly type = IPC_DATA_TYPE.STREAM_END;
  constructor(readonly stream_id: string) {}
}
