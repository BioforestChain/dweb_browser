type $FetchExtends<E> = E & ThisType<Promise<Response> & E>;
export declare const fetchStreamExtends: $FetchExtends<{
    /** 将响应的内容解码成 jsonlines 格式 */
    jsonlines<T = unknown>(): Promise<ReadableStream<T>>;
    /** 获取 Response 的 body 为 ReadableStream */
    stream(): Promise<ReadableStream<Uint8Array>>;
}>;
export {};
