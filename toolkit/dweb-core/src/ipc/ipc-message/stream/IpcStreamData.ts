import type { OrderBy } from "@dweb-browser/helper/OrderBy.ts";
import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { simpleDecoder } from "@dweb-browser/helper/encoding.ts";
import { stringHashCode } from "@dweb-browser/helper/hashCode.ts";
import { CUSTOM_INSPECT } from "@dweb-browser/helper/logger.ts";
import { $dataToBinary, $dataToText, IPC_DATA_ENCODING } from "../internal/IpcData.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "../internal/IpcMessage.ts";

export type $IpcStreamData = ReturnType<typeof ipcStreamData>;
const ipcStreamData = (stream_id: string, data: string | Uint8Array, encoding: IPC_DATA_ENCODING, order: number) =>
  ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_DATA), stream_id, data, encoding, order } as const satisfies OrderBy);
export const IpcStreamData = Object.assign(ipcStreamData, {
  fromBase64(
    stream_id: string,
    data: Uint8Array,
    order: number = stringHashCode(stream_id)
  ): { [key: string | symbol]: unknown } {
    return Object.assign(ipcStreamData(stream_id, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64, order), {
      // 打印的时候只会输出前20位和后20位，中间以 "..." 省略
      [CUSTOM_INSPECT]() {
        return JSON.stringify({
          ...this,
          data: data.length <= 100 ? data : `${data.slice(0, 20)}...${data.slice(-20)}`,
        });
      },
    });
  },
  fromBinary(stream_id: string, data: Uint8Array, order: number = stringHashCode(stream_id)) {
    return Object.assign(ipcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY, order), {
      toJSON: $once(() => IpcStreamData.fromBase64(stream_id, data, order)),
    });
  },
  fromUtf8(stream_id: string, data: Uint8Array, order: number = stringHashCode(stream_id)) {
    return ipcStreamData(stream_id, simpleDecoder(data, "utf8"), IPC_DATA_ENCODING.UTF8, order);
  },
  binary(streamData: $IpcStreamData) {
    return $dataToBinary(streamData.data, streamData.encoding);
  },
  text(streamData: $IpcStreamData) {
    return $dataToText(streamData.data, streamData.encoding);
  },
});
