import { CacheGetter } from "../../../helper/cacheGetter.ts";
import { simpleDecoder } from "../../../helper/encoding.ts";
import { $dataToBinary, $dataToText, IPC_DATA_ENCODING, IPC_MESSAGE_TYPE } from "../helper/const.ts";
import { IpcMessage } from "../ipc-message/IpcMessage.ts";

export class IpcStreamData extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_DATA> {
  constructor(readonly stream_id: string, readonly data: string | Uint8Array, readonly encoding: IPC_DATA_ENCODING) {
    super(IPC_MESSAGE_TYPE.STREAM_DATA);
  }
  static fromBase64(stream_id: string, data: Uint8Array) {
    return new IpcStreamData(stream_id, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64);
  }
  static fromBinary(stream_id: string, data: Uint8Array) {
    return new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY);
  }
  static fromUtf8(stream_id: string, data: Uint8Array) {
    return new IpcStreamData(stream_id, simpleDecoder(data, "utf8"), IPC_DATA_ENCODING.UTF8);
  }

  #binary = new CacheGetter(() => $dataToBinary(this.data, this.encoding));
  get binary() {
    return this.#binary.value;
  }

  #text = new CacheGetter(() => $dataToText(this.data, this.encoding));
  get text() {
    return this.#text.value;
  }

  #jsonAble = new CacheGetter(() => {
    if (this.encoding === IPC_DATA_ENCODING.BINARY) {
      return IpcStreamData.fromBase64(this.stream_id, this.data as Uint8Array);
    }
    return this as IpcStreamData;
  });
  get jsonAble(): IpcStreamData {
    return this.#jsonAble.value;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
}
