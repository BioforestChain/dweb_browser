export declare const fetchExtends: {
    jsonlines<T = unknown>(): Promise<ReadableStream<T>>;
    stream(): Promise<ReadableStream<Uint8Array>>;
    number(): Promise<number>;
    ok(): Promise<Response>;
    text(): Promise<string>;
    binary(): Promise<ArrayBuffer>;
    boolean(): Promise<boolean>;
    object<T_1>(): Promise<T_1>;
};
