import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { simpleDecoder } from "@dweb-browser/helper/encoding.ts";
import { $dataToBinary, $dataToText, IPC_DATA_ENCODING } from "./internal/IpcData.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";

export type $IpcEvent<N extends string = string> = ReturnType<typeof ipcEvent<N>>;

const ipcEvent = <N extends string>(
  name: N,
  data: string | Uint8Array,
  encoding: IPC_DATA_ENCODING,
  orderBy?: number
) =>
  ({
    ...ipcMessageBase(IPC_MESSAGE_TYPE.EVENT),
    name,
    data,
    encoding,
    orderBy,
  } as const);

export const IpcEvent = Object.assign(ipcEvent, {
  fromBase64(name: string, data: Uint8Array, orderBy?: number) {
    return ipcEvent(name, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64, orderBy);
  },
  fromBinary(name: string, data: Uint8Array, orderBy?: number) {
    return Object.assign(ipcEvent(name, data, IPC_DATA_ENCODING.BINARY, orderBy), {
      toJSON: $once(() => IpcEvent.fromBase64(name, data, orderBy)),
    });
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
