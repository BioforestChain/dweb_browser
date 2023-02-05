import { IPC_DATA_TYPE } from "./const.cjs";

export class IpcStreamData {
  readonly type = IPC_DATA_TYPE.STREAM_DATA;
  constructor(readonly stream_id: string, readonly data: string | Uint8Array) {}
}
