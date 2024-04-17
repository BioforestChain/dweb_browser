import { simpleDecoder } from "../../../helper/encoding.ts";
import { $dataToBinary, $dataToText } from "../helper/const.ts";
import { IPC_DATA_ENCODING } from "./internal/IpcData.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";

export type $IpcEvent = ReturnType<typeof _ipcEvent>;

const _ipcEvent = (name: string, data: string | Uint8Array, encoding: IPC_DATA_ENCODING, orderBy?: number) =>
  ({
    ...ipcMessageBase(IPC_MESSAGE_TYPE.EVENT),
    name,
    data,
    encoding,
    orderBy,
  } as const);

export const ipcEvent = Object.assign(_ipcEvent, {
  fromBase64(name: string, data: Uint8Array, orderBy?: number) {
    return ipcEvent(name, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64, orderBy);
  },
  fromBinary(name: string, data: Uint8Array, orderBy?: number) {
    return ipcEvent(name, data, IPC_DATA_ENCODING.BINARY, orderBy);
  },
  fromUtf8(name: string, data: Uint8Array, orderBy?: number) {
    return ipcEvent(name, simpleDecoder(data, "utf8"), IPC_DATA_ENCODING.UTF8, orderBy);
  },
  fromText(name: string, data: string, orderBy?: number) {
    return ipcEvent(name, data, IPC_DATA_ENCODING.UTF8, orderBy);
  },
  binary(event: $IpcEvent) {
    return $dataToBinary(event.data, event.encoding);
  },
  text(event: $IpcEvent) {
    return $dataToText(event.data, event.encoding);
  },
});
