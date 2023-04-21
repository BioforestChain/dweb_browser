import { cacheGetter } from "../../helper/cacheGetter.cjs";
import { simpleDecoder } from "../../helper/encoding.cjs";
import {
  $dataToBinary,
  $dataToText,
  IpcMessage,
  IPC_DATA_ENCODING,
  IPC_MESSAGE_TYPE,
} from "./const.cjs";

export class IpcStreamData extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_DATA> {
  constructor(
    readonly stream_id: string,
    readonly data: string | Uint8Array,
    readonly encoding: IPC_DATA_ENCODING
  ) {
    super(IPC_MESSAGE_TYPE.STREAM_DATA);
  }
  static fromBase64(stream_id: string, data: Uint8Array) {
    return new IpcStreamData(
      stream_id,
      simpleDecoder(data, "base64"),
      IPC_DATA_ENCODING.BASE64
    );
  }
  static fromBinary(stream_id: string, data: Uint8Array) {
    return new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY);
  }
  static fromUtf8(stream_id: string, data: Uint8Array) {
    return new IpcStreamData(
      stream_id,
      simpleDecoder(data, "utf8"),
      IPC_DATA_ENCODING.UTF8
    );
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
  get jsonAble(): IpcStreamData {
    if (this.encoding === IPC_DATA_ENCODING.BINARY) {
      return IpcStreamData.fromBase64(this.stream_id, this.data as Uint8Array);
    }
    return this;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
}
