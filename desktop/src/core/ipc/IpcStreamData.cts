import { simpleDecoder } from "../../helper/encoding.cjs";
import { IpcMessage, IPC_DATA_TYPE } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";

export class IpcStreamData extends IpcMessage<IPC_DATA_TYPE.STREAM_DATA> {
  constructor(readonly stream_id: string, readonly data: string | Uint8Array) {
    super(IPC_DATA_TYPE.STREAM_DATA);
  }
  static fromBinary(ipc: Ipc, stream_id: string, data: Uint8Array) {
    if (ipc.support_binary) {
      return new IpcStreamData(stream_id, data);
    }
    return new IpcStreamData(stream_id, simpleDecoder(data, "base64"));
  }
}
