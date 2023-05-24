import { cacheGetter } from "../../helper/cacheGetter.js";
import { simpleDecoder } from "../../helper/encoding.js";
import {
  $dataToBinary,
  $dataToText,
  IpcMessage,
  IPC_DATA_ENCODING,
  IPC_MESSAGE_TYPE,
} from "./const.js";

export class IpcEvent extends IpcMessage<IPC_MESSAGE_TYPE.EVENT> {
  constructor(
    readonly name: string,
    readonly data: string | Uint8Array,
    readonly encoding: IPC_DATA_ENCODING
  ) {
    super(IPC_MESSAGE_TYPE.EVENT);
  }
  static fromBase64(name: string, data: Uint8Array) {
    return new IpcEvent(
      name,
      simpleDecoder(data, "base64"),
      IPC_DATA_ENCODING.BASE64
    );
  }
  static fromBinary(name: string, data: Uint8Array) {
    return new IpcEvent(name, data, IPC_DATA_ENCODING.BINARY);
  }
  static fromUtf8(name: string, data: Uint8Array) {
    return new IpcEvent(
      name,
      simpleDecoder(data, "utf8"),
      IPC_DATA_ENCODING.UTF8
    );
  }
  static fromText(name: string, data: string) {
    return new IpcEvent(name, data, IPC_DATA_ENCODING.UTF8);
  }

  @cacheGetter()
  get binary() {
    return $dataToBinary(this.data, this.encoding);
  }

  @cacheGetter()
  get text() {
    return $dataToText(this.data, this.encoding);
  }

  @cacheGetter()
  get jsonAble(): IpcEvent {
    if (this.encoding === IPC_DATA_ENCODING.BINARY) {
      return IpcEvent.fromBase64(this.name, this.data as Uint8Array);
    }
    return this;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
}
