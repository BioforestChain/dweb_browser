type $FetchExtends<E> = E & ThisType<Promise<Response> & E>;
export declare const fetchBaseExtends: $FetchExtends<{
    number(): Promise<number>;
    ok(): Promise<Response>;
    text(): Promise<string>;
    binary(): Promise<ArrayBuffer>;
    boolean(): Promise<boolean>;
    object<T>(): Promise<T>;
}>;
export {};
