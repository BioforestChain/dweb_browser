import { simpleDecoder, simpleEncoder } from "../../helper/encoding.cjs";
import { IpcMessage, IPC_DATA_ENCODING, IPC_MESSAGE_TYPE } from "./const.cjs";

export class IpcStreamData extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_DATA> {
  constructor(
    readonly stream_id: string,
    readonly data: string | Uint8Array,
    readonly encoding: IPC_DATA_ENCODING
  ) {
    super(IPC_MESSAGE_TYPE.STREAM_DATA);
  }
  static asBase64(stream_id: string, data: Uint8Array) {
    return new IpcStreamData(
      stream_id,
      simpleDecoder(data, "base64"),
      IPC_DATA_ENCODING.BASE64
    );
  }
  static asBinary(stream_id: string, data: Uint8Array) {
    return new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY);
  }
  static asUtf8(stream_id: string, data: Uint8Array) {
    return new IpcStreamData(
      stream_id,
      simpleDecoder(data, "utf8"),
      IPC_DATA_ENCODING.UTF8
    );
  }

  get binary() {
    switch (this.encoding) {
      case IPC_DATA_ENCODING.BINARY: {
        return this.data as Uint8Array;
      }
      case IPC_DATA_ENCODING.BASE64: {
        return simpleEncoder(this.data as string, "base64");
      }
      case IPC_DATA_ENCODING.UTF8: {
        return simpleEncoder(this.data as string, "utf8");
      }
    }
  }
}
