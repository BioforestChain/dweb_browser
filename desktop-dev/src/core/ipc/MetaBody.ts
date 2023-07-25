import { CacheGetter } from "../../helper/cacheGetter.ts";
import { simpleDecoder } from "../../helper/encoding.ts";
import type { $JSON } from "../ipc-web/$messageToIpcMessage.ts";
import { IPC_DATA_ENCODING } from "./const.ts";
import type { Ipc } from "./ipc.ts";

export class MetaBody {
  constructor(
    readonly type: IPC_META_BODY_TYPE,
    readonly senderUid: number,
    readonly data: string | Uint8Array,
    readonly streamId?: string,
    public receiverUid?: number,
    readonly metaId = simpleDecoder(crypto.getRandomValues(new Uint8Array(8)), "base64")
  ) {}
  static fromJSON(metaBody: MetaBody | $JSON<MetaBody>) {
    if (metaBody instanceof MetaBody === false) {
      metaBody = new MetaBody(
        metaBody.type,
        metaBody.senderUid,
        metaBody.data,
        metaBody.streamId,
        metaBody.receiverUid,
        metaBody.metaId
      );
    }
    return metaBody as MetaBody;
  }

  static fromText(senderUid: number, data: string, streamId?: string, receiverUid?: number) {
    return new MetaBody(
      streamId == null ? IPC_META_BODY_TYPE.INLINE_TEXT : IPC_META_BODY_TYPE.STREAM_WITH_TEXT,
      senderUid,
      data,
      streamId,
      receiverUid
    );
  }
  static fromBase64(senderUid: number, data: string, streamId?: string, receiverUid?: number) {
    return new MetaBody(
      streamId == null ? IPC_META_BODY_TYPE.INLINE_BASE64 : IPC_META_BODY_TYPE.STREAM_WITH_BASE64,
      senderUid,
      data,
      streamId,
      receiverUid
    );
  }
  static fromBinary(sender: Ipc | number, data: Uint8Array, streamId?: string, receiverUid?: number): MetaBody {
    if (typeof sender === "number") {
      return new MetaBody(
        streamId == null ? IPC_META_BODY_TYPE.INLINE_BINARY : IPC_META_BODY_TYPE.STREAM_WITH_BINARY,
        sender,
        data,
        streamId,
        receiverUid
      );
    }
    if (sender.support_binary) {
      return this.fromBinary(sender.uid, data, streamId, receiverUid);
    }
    return this.fromBase64(sender.uid, simpleDecoder(data, "base64"), streamId, receiverUid);
  }
  #type_encoding = new CacheGetter(() => {
    const encoding = this.type & 0b11111110;
    switch (encoding) {
      case IPC_DATA_ENCODING.UTF8:
        return IPC_DATA_ENCODING.UTF8;
      case IPC_DATA_ENCODING.BASE64:
        return IPC_DATA_ENCODING.BASE64;
      case IPC_DATA_ENCODING.BINARY:
        return IPC_DATA_ENCODING.BINARY;
      default:
        return IPC_DATA_ENCODING.UTF8;
    }
  });
  get type_encoding() {
    return this.#type_encoding.value;
  }
  #type_isInline = new CacheGetter(() => (this.type & IPC_META_BODY_TYPE.INLINE) !== 0);
  get type_isInline() {
    return this.#type_isInline.value;
  }
  #type_isStream = new CacheGetter(() => (this.type & IPC_META_BODY_TYPE.INLINE) === 0);
  get type_isStream() {
    return this.#type_isStream.value;
  }
  #jsonAble = new CacheGetter(() => {
    if (this.type_encoding === IPC_DATA_ENCODING.BINARY) {
      return MetaBody.fromBase64(
        this.senderUid,
        simpleDecoder(this.data as Uint8Array, "base64"),
        this.streamId,
        this.receiverUid
      );
    }
    return this as MetaBody;
  });
  get jsonAble(): MetaBody {
    return this.#jsonAble.value;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
}
export const enum IPC_META_BODY_TYPE {
  /** 流 */
  STREAM_ID = 0,
  /** 内联数据 */
  INLINE = 1,
  /** 流，但是携带一帧的 UTF8 数据 */
  STREAM_WITH_TEXT = IPC_META_BODY_TYPE.STREAM_ID | IPC_DATA_ENCODING.UTF8,
  /** 流，但是携带一帧的 BASE64 数据 */
  STREAM_WITH_BASE64 = IPC_META_BODY_TYPE.STREAM_ID | IPC_DATA_ENCODING.BASE64,
  /** 流，但是携带一帧的 BINARY 数据 */
  STREAM_WITH_BINARY = IPC_META_BODY_TYPE.STREAM_ID | IPC_DATA_ENCODING.BINARY,
  /** 内联 UTF8 数据 */
  INLINE_TEXT = IPC_META_BODY_TYPE.INLINE | IPC_DATA_ENCODING.UTF8,
  /** 内联 BASE64 数据 */
  INLINE_BASE64 = IPC_META_BODY_TYPE.INLINE | IPC_DATA_ENCODING.BASE64,
  /** 内联 BINARY 数据 */
  INLINE_BINARY = IPC_META_BODY_TYPE.INLINE | IPC_DATA_ENCODING.BINARY,
}
