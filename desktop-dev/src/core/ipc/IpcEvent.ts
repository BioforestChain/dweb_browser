import { CacheGetter } from "../../helper/cacheGetter.ts";
import { simpleDecoder } from "../../helper/encoding.ts";
import { $dataToBinary, $dataToText, IPC_DATA_ENCODING, IPC_MESSAGE_TYPE, IpcMessage } from "./const.ts";

export class IpcEvent extends IpcMessage<IPC_MESSAGE_TYPE.EVENT> {
  constructor(readonly name: string, readonly data: string | Uint8Array, readonly encoding: IPC_DATA_ENCODING) {
    super(IPC_MESSAGE_TYPE.EVENT);
  }
  static fromBase64(name: string, data: Uint8Array) {
    return new IpcEvent(name, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64);
  }
  static fromBinary(name: string, data: Uint8Array) {
    return new IpcEvent(name, data, IPC_DATA_ENCODING.BINARY);
  }
  static fromUtf8(name: string, data: Uint8Array) {
    return new IpcEvent(name, simpleDecoder(data, "utf8"), IPC_DATA_ENCODING.UTF8);
  }
  static fromText(name: string, data: string) {
    return new IpcEvent(name, data, IPC_DATA_ENCODING.UTF8);
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
      return IpcEvent.fromBase64(this.name, this.data as Uint8Array);
    }
    return this as IpcEvent;
  });
  get jsonAble() {
    return this.#jsonAble.value;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
}
