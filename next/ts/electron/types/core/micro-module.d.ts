import type { $IpcSupportProtocols, $MicroModule, $MMID } from "../helper/types.js";
import type { $BootstrapContext } from "./bootstrapContext.js";
import type { Ipc } from "./ipc/index.js";
export declare abstract class MicroModule implements $MicroModule {
    abstract ipc_support_protocols: $IpcSupportProtocols;
    abstract mmid: $MMID;
    protected abstract _bootstrap(context: $BootstrapContext): unknown;
    protected abstract _shutdown(): unknown;
    private _running_state_lock;
    protected readonly _after_shutdown_signal: import("../helper/createSignal.js").Signal<() => unknown>;
    protected _ipcSet: Set<Ipc>;
    /**
     * 内部程序与外部程序通讯的方法
     * TODO 这里应该是可以是多个
     */
    private readonly _connectSignal;
    get isRunning(): Promise<boolean>;
    protected before_bootstrap(context: $BootstrapContext): Promise<void>;
    protected after_bootstrap(context: $BootstrapContext): Promise<void>;
    bootstrap(context: $BootstrapContext): Promise<void>;
    protected before_shutdown(): Promise<void>;
    protected after_shutdown(): void;
    shutdown(): Promise<void>;
    /**
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     */
    protected onConnect(cb: $OnIpcConnect): import("../helper/createSignal.js").$OffListener;
    beConnect(ipc: Ipc, reason: Request): Promise<void>;
    private _nativeFetch;
    nativeFetch(url: RequestInfo | URL, init?: RequestInit): Promise<Response> & {
        jsonlines<T = unknown>(): Promise<ReadableStream<T>>;
        stream(): Promise<ReadableStream<Uint8Array>>;
        number(): Promise<number>;
        ok(): Promise<Response>;
        text(): Promise<string>;
        binary(): Promise<ArrayBuffer>;
        boolean(): Promise<boolean>;
        object<T_1>(): Promise<T_1>;
    };
}
type $OnIpcConnect = (ipc: Ipc, reason: Request) => unknown;
export {};
