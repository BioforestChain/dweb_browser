import type { OrderBy } from "@dweb-browser/helper/OrderBy.ts";
import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { simpleDecoder } from "@dweb-browser/helper/encoding.ts";
import { $dataToBinary, $dataToText, IPC_DATA_ENCODING } from "./internal/IpcData.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";

export type $IpcEvent<N extends string = string> = ReturnType<typeof ipcEvent<N>>;

const ipcEvent = <N extends string>(name: N, data: string | Uint8Array, encoding: IPC_DATA_ENCODING, order?: number) =>
  ({
    ...ipcMessageBase(IPC_MESSAGE_TYPE.EVENT),
    name,
    data,
    encoding,
    order,
  } as const satisfies OrderBy);

export const IpcEvent = Object.assign(ipcEvent, {
  fromBase64(name: string, data: Uint8Array, order?: number) {
    return ipcEvent(name, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64, order);
  },
  fromBinary(name: string, data: Uint8Array, order?: number) {
    return Object.assign(ipcEvent(name, data, IPC_DATA_ENCODING.BINARY, order), {
      toJSON: $once(() => IpcEvent.fromBase64(name, data, order)),
    });
  },
  fromUtf8(name: string, data: Uint8Array, order?: number) {
    return ipcEvent(name, simpleDecoder(data, "utf8"), IPC_DATA_ENCODING.UTF8, order);
  },
  fromText(name: string, data: string, order?: number) {
    return ipcEvent(name, data, IPC_DATA_ENCODING.UTF8, order);
  },
  binary(event: $IpcEvent) {
    return $dataToBinary(event.data, event.encoding);
  },
  text(event: $IpcEvent) {
    return $dataToText(event.data, event.encoding);
  },
});
