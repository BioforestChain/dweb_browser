/**
 * @param value
 * @returns
 * @inline
 */
export declare const isPromiseLike: <T extends unknown = unknown>(value: unknown) => value is PromiseLike<Awaited<T>>;
/**
 * @param value
 * @returns
 * @inline
 */
export declare const isPromise: <T extends unknown = unknown>(value: unknown) => value is Promise<Awaited<T>>;
type $InnerThen<T> = (result: T) => unknown;
type $InnerCatch = (reason?: unknown) => unknown;
export declare class PromiseOut<T = unknown> {
    static resolve<T>(v: T): PromiseOut<T>;
    static sleep(ms: number): PromiseOut<void>;
    promise: Promise<T>;
    is_resolved: boolean;
    is_rejected: boolean;
    is_finished: boolean;
    value?: T;
    reason?: unknown;
    resolve: (value: T | PromiseLike<T>) => void;
    reject: (reason?: unknown) => void;
    private _innerFinally?;
    private _innerFinallyArg?;
    private _innerThen?;
    private _innerCatch?;
    constructor();
    onSuccess(innerThen: $InnerThen<T>): void;
    onError(innerCatch: $InnerCatch): void;
    onFinished(innerFinally: () => unknown): void;
    private _runFinally;
    private __callInnerFinally;
    private _runThen;
    private _runCatch;
    private __callInnerThen;
    private __callInnerCatch;
}
export {};
