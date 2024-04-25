import { simpleDecoder } from "../../../../helper/encoding.ts";
import { $dataToBinary, $dataToText, IPC_DATA_ENCODING } from "../internal/IpcData.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "../internal/IpcMessage.ts";

export type $IpcStreamData = ReturnType<typeof _ipcStreamData>;
const _ipcStreamData = (stream_id: string, data: string | Uint8Array, encoding: IPC_DATA_ENCODING) =>
  ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_DATA), stream_id, data, encoding } as const);
export const ipcStreamData = Object.assign(_ipcStreamData, {
  fromBase64(stream_id: string, data: Uint8Array) {
    return ipcStreamData(stream_id, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64);
  },
  fromBinary(stream_id: string, data: Uint8Array) {
    return ipcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY);
  },
  fromUtf8(stream_id: string, data: Uint8Array) {
    return ipcStreamData(stream_id, simpleDecoder(data, "utf8"), IPC_DATA_ENCODING.UTF8);
  },
  binary(streamData: $IpcStreamData) {
    return $dataToBinary(streamData.data, streamData.encoding);
  },
  text(streamData: $IpcStreamData) {
    return $dataToText(streamData.data, streamData.encoding);
  },
});
