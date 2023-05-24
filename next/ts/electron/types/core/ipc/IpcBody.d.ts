import type { MetaBody } from "./MetaBody.js";
export declare abstract class IpcBody {
    static wm: WeakMap<Uint8Array | ReadableStream<any>, IpcBody>;
    abstract readonly metaBody: MetaBody;
    protected abstract _bodyHub: BodyHub;
    get raw(): $BodyData;
    u8a(): Promise<Uint8Array>;
    stream(): Promise<ReadableStream<Uint8Array>>;
    text(): Promise<string>;
}
export declare class BodyHub {
    readonly data: $BodyData;
    constructor(data: $BodyData);
    u8a?: Uint8Array;
    stream?: ReadableStream<Uint8Array>;
    text?: string;
}
export type $BodyData = Uint8Array | ReadableStream<Uint8Array> | string;
