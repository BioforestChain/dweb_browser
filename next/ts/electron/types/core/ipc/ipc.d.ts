import { PromiseOut } from "../../helper/PromiseOut.js";
import type { $IpcMicroModuleInfo } from "../../helper/types.js";
import { MicroModule } from "../micro-module.js";
import { $IpcMessage, $OnIpcEventMessage, $OnIpcRequestMessage, $OnIpcStreamMessage, type $OnIpcMessage } from "./const.js";
import type { IpcHeaders } from "./IpcHeaders.js";
import type { IpcResponse } from "./IpcResponse.js";
export declare abstract class Ipc {
    readonly uid: number;
    /**
     * 是否支持使用 MessagePack 直接传输二进制
     * 在一些特殊的场景下支持字符串传输，比如与webview的通讯
     * 二进制传输在网络相关的服务里被支持，里效率会更高，但前提是对方有 MessagePack 的编解码能力
     * 否则 JSON 是通用的传输协议
     */
    get support_message_pack(): boolean;
    protected _support_message_pack: boolean;
    /**
     * 是否支持使用 Protobuf 直接传输二进制
     * 在网络环境里，protobuf 是更加高效的协议
     */
    get support_protobuf(): boolean;
    protected _support_protobuf: boolean;
    /**
     * 是否支持结构化内存协议传输：
     * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
     */
    get support_raw(): boolean;
    protected _support_raw: boolean;
    /**
     * 是否支持二进制传输
     */
    get support_binary(): boolean;
    protected _support_binary: boolean;
    abstract readonly remote: $IpcMicroModuleInfo;
    asRemoteInstance(): MicroModule | undefined;
    abstract readonly role: string;
    protected _messageSignal: import("../../helper/createSignal.js").Signal<$OnIpcMessage>;
    postMessage(message: $IpcMessage): void;
    abstract _doPostMessage(data: $IpcMessage): void;
    onMessage: (cb: $OnIpcMessage) => import("../../helper/createSignal.js").$OffListener;
    private get _onRequestSignal();
    onRequest(cb: $OnIpcRequestMessage): import("../../helper/createSignal.js").$OffListener;
    private get _onStreamSignal();
    onStream(cb: $OnIpcStreamMessage): import("../../helper/createSignal.js").$OffListener;
    private get _onEventSignal();
    onEvent(cb: $OnIpcEventMessage): import("../../helper/createSignal.js").$OffListener;
    abstract _doClose(): void;
    private _closed;
    close(): void;
    private _closeSignal;
    onClose: (cb: () => unknown) => import("../../helper/createSignal.js").$OffListener;
    private _req_id_acc;
    allocReqId(url?: string): number;
    private get _reqresMap();
    /** 发起请求并等待响应 */
    request(url: string, init?: {
        method?: string;
        body?: string | Uint8Array | ReadableStream<Uint8Array>;
        headers?: IpcHeaders | HeadersInit;
    }): Promise<IpcResponse>;
    /** 自定义注册 请求与响应 的id */
    registerReqId(req_id?: number): PromiseOut<IpcResponse>;
}
