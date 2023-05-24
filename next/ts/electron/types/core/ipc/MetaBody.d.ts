import type { $JSON } from "../ipc-web/$messageToIpcMessage.js";
import { IPC_DATA_ENCODING } from "./const.js";
import type { Ipc } from "./ipc.js";
export declare class MetaBody {
    readonly type: IPC_META_BODY_TYPE;
    readonly senderUid: number;
    readonly data: string | Uint8Array;
    readonly streamId?: string | undefined;
    receiverUid?: number | undefined;
    readonly metaId: string;
    constructor(type: IPC_META_BODY_TYPE, senderUid: number, data: string | Uint8Array, streamId?: string | undefined, receiverUid?: number | undefined, metaId?: string);
    static fromJSON(metaBody: MetaBody | $JSON<MetaBody>): MetaBody | $JSON<MetaBody>;
    static fromText(senderUid: number, data: string, streamId?: string, receiverUid?: number): MetaBody;
    static fromBase64(senderUid: number, data: string, streamId?: string, receiverUid?: number): MetaBody;
    static fromBinary(sender: Ipc | number, data: Uint8Array, streamId?: string, receiverUid?: number): MetaBody;
    get type_encoding(): IPC_DATA_ENCODING | undefined;
    get type_isInline(): boolean;
    get type_isStream(): boolean;
    get jsonAble(): MetaBody;
    toJSON(): {
        type: IPC_META_BODY_TYPE;
        senderUid: number;
        data: string | Uint8Array;
        streamId?: string | undefined;
        receiverUid?: number | undefined;
        metaId: string;
    };
}
export declare const enum IPC_META_BODY_TYPE {
    /** 流 */
    STREAM_ID = 0,
    /** 内联数据 */
    INLINE = 1,
    /** 流，但是携带一帧的 UTF8 数据 */
    STREAM_WITH_TEXT = 2,
    /** 流，但是携带一帧的 BASE64 数据 */
    STREAM_WITH_BASE64 = 4,
    /** 流，但是携带一帧的 BINARY 数据 */
    STREAM_WITH_BINARY = 8,
    /** 内联 UTF8 数据 */
    INLINE_TEXT = 3,
    /** 内联 BASE64 数据 */
    INLINE_BASE64 = 5,
    /** 内联 BINARY 数据 */
    INLINE_BINARY = 9
}
