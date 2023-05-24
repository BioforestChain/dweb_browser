import { $Callback } from "./createSignal.js";
export declare const streamRead: <T extends unknown>(stream: ReadableStream<T>, options?: {
    signal?: AbortSignal;
}) => AsyncGenerator<Awaited<T>, void, unknown>;
export declare const binaryStreamRead: (stream: ReadableStream<Uint8Array>, options?: {
    signal?: AbortSignal;
}) => AsyncGenerator<Uint8Array, void, unknown> & {
    available: () => Promise<number>;
    readBinary: (size: number) => Promise<Uint8Array>;
    readInt: () => Promise<number>;
};
export declare const streamReadAll: <I extends unknown, T, R>(stream: ReadableStream<I>, options?: {
    map?: ((item: I) => T) | undefined;
    complete?: ((items: I[], maps: T[]) => R) | undefined;
}) => Promise<{
    items: I[];
    maps: T[];
    result: R;
}>;
export declare const streamReadAllBuffer: (stream: ReadableStream<Uint8Array>) => Promise<Uint8Array>;
export declare class ReadableStreamOut<T> {
    readonly strategy?: QueuingStrategy<T> | undefined;
    constructor(strategy?: QueuingStrategy<T> | undefined);
    controller: ReadableStreamDefaultController<T>;
    stream: ReadableStream<T>;
    private _on_cancel_signal?;
    get onCancel(): (cb: $Callback<[any]>) => import("./createSignal.js").$OffListener;
    private _on_pull_signal?;
    get onPull(): (cb: $OnPull) => import("./createSignal.js").$OffListener;
}
export type $OnPull = () => unknown;
export declare const streamFromCallback: <T extends (...args: any[]) => unknown>(cb: T, onCancel?: Promise<unknown>) => ReadableStream<Parameters<T>>;
