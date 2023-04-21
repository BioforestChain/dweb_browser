import { cacheGetter } from "../../helper/cacheGetter.cjs";
import { simpleDecoder } from "../../helper/encoding.cjs";
import type { $JSON } from "../ipc-web/$messageToIpcMessage.cjs";
import { IPC_DATA_ENCODING } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";

export class MetaBody {
  constructor(
    readonly type: IPC_META_BODY_TYPE,
    readonly senderUid: number,
    readonly data: string | Uint8Array,
    readonly streamId?: string,
    public receiverUid?: number,
    readonly metaId = simpleDecoder(
      crypto.getRandomValues(new Uint8Array(8)),
      "base64"
    )
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
    return metaBody;
  }

  static fromText(
    senderUid: number,
    data: string,
    streamId?: string,
    receiverUid?: number
  ) {
    return new MetaBody(
      streamId == null
        ? IPC_META_BODY_TYPE.INLINE_TEXT
        : IPC_META_BODY_TYPE.STREAM_WITH_TEXT,
      senderUid,
      data,
      streamId,
      receiverUid
    );
  }
  static fromBase64(
    senderUid: number,
    data: string,
    streamId?: string,
    receiverUid?: number
  ) {
    return new MetaBody(
      streamId == null
        ? IPC_META_BODY_TYPE.INLINE_BASE64
        : IPC_META_BODY_TYPE.STREAM_WITH_BASE64,
      senderUid,
      data,
      streamId,
      receiverUid
    );
  }
  static fromBinary(
    sender: Ipc | number,
    data: Uint8Array,
    streamId?: string,
    receiverUid?: number
  ): MetaBody {
    if (typeof sender === "number") {
      return new MetaBody(
        streamId == null
          ? IPC_META_BODY_TYPE.INLINE_BINARY
          : IPC_META_BODY_TYPE.STREAM_WITH_BINARY,
        sender,
        data,
        streamId,
        receiverUid
      );
    }
    if (sender.support_binary) {
      return this.fromBinary(sender.uid, data, streamId, receiverUid);
    }
    return this.fromBase64(
      sender.uid,
      simpleDecoder(data, "base64"),
      streamId,
      receiverUid
    );
  }
  @cacheGetter()
  get type_encoding() {
    const encoding = this.type & 0b11111110;
    switch (encoding) {
      case IPC_DATA_ENCODING.UTF8:
        return IPC_DATA_ENCODING.UTF8;
      case IPC_DATA_ENCODING.BASE64:
        return IPC_DATA_ENCODING.BASE64;
      case IPC_DATA_ENCODING.BINARY:
        return IPC_DATA_ENCODING.BINARY;
    }
  }
  @cacheGetter()
  get type_isInline() {
    return (this.type & IPC_META_BODY_TYPE.INLINE) !== 0;
  }
  @cacheGetter()
  get type_isStream() {
    return (this.type & IPC_META_BODY_TYPE.INLINE) === 0;
  }
  @cacheGetter()
  get jsonAble(): MetaBody {
    if (this.type_encoding === IPC_DATA_ENCODING.BINARY) {
      return MetaBody.fromBase64(
        this.senderUid,
        simpleDecoder(this.data as Uint8Array, "base64"),
        this.streamId,
        this.receiverUid
      );
    }
    return this;
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
