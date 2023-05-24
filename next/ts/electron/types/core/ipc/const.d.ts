import type { Ipc } from "./ipc.js";
import type { IpcEvent } from "./IpcEvent.js";
import type { IpcReqMessage, IpcRequest } from "./IpcRequest.js";
import type { IpcResMessage, IpcResponse } from "./IpcResponse.js";
import type { IpcStreamAbort } from "./IpcStreamAbort.js";
import type { IpcStreamData } from "./IpcStreamData.js";
import type { IpcStreamEnd } from "./IpcStreamEnd.js";
import type { IpcStreamPaused } from "./IpcStreamPaused.js";
import type { IpcStreamPulling } from "./IpcStreamPulling.js";
export declare const enum IPC_METHOD {
    GET = "GET",
    POST = "POST",
    PUT = "PUT",
    DELETE = "DELETE",
    OPTIONS = "OPTIONS",
    TRACE = "TRACE",
    PATCH = "PATCH",
    PURGE = "PURGE",
    HEAD = "HEAD"
}
export declare const toIpcMethod: (method?: string) => IPC_METHOD;
export declare const enum IPC_MESSAGE_TYPE {
    /** 类型：请求 */
    REQUEST = 0,
    /** 类型：相应 */
    RESPONSE = 1,
    /** 类型：流数据，发送方 */
    STREAM_DATA = 2,
    /** 类型：流拉取，请求方
     * 发送方一旦收到该指令，就可以持续发送数据
     * 该指令中可以携带一些“限流协议信息”，如果违背该协议，请求方可能会断开连接
     */
    STREAM_PULLING = 3,
    /** 类型：流暂停，请求方
     * 发送方一旦收到该指令，就应当停止基本的数据发送
     * 该指令中可以携带一些“保险协议信息”，描述仍然允许发送的一些数据类型、发送频率等。如果违背该协议，请求方可以会断开连接
     */
    STREAM_PAUSED = 4,
    /** 类型：流关闭，发送方
     * 可能是发送完成了，也有可能是被中断了
     */
    STREAM_END = 5,
    /** 类型：流中断，请求方 */
    STREAM_ABORT = 6,
    /** 类型：事件 */
    EVENT = 7
}
/**
 * 数据编码格式
 */
export declare const enum IPC_DATA_ENCODING {
    /** 文本 json html 等 */
    UTF8 = 2,
    /** 使用文本表示的二进制 */
    BASE64 = 4,
    /** 二进制 */
    BINARY = 8
}
export declare const enum IPC_ROLE {
    SERVER = "server",
    CLIENT = "client"
}
export declare class IpcMessage<T extends IPC_MESSAGE_TYPE> {
    readonly type: T;
    constructor(type: T);
}
/** 接收到的消息，可传输的数据 */
export type $IpcTransferableMessage = IpcReqMessage | IpcResMessage | IpcEvent | $IpcStreamMessage;
/** 发送的消息 */
export type $IpcMessage = IpcRequest | IpcResponse | IpcEvent | $IpcStreamMessage;
export type $IpcStreamMessage = IpcStreamData | IpcStreamPulling | IpcStreamPaused | IpcStreamEnd | IpcStreamAbort;
export type $OnIpcMessage = (message: $IpcMessage, ipc: Ipc) => unknown;
export type $OnIpcRequestMessage = (message: IpcRequest, ipc: Ipc) => unknown;
export type $OnIpcEventMessage = (message: IpcEvent, ipc: Ipc) => unknown;
export type $OnIpcStreamMessage = (message: $IpcStreamMessage, ipc: Ipc) => unknown;
export declare const $dataToBinary: (data: string | Uint8Array, encoding: IPC_DATA_ENCODING) => Uint8Array;
export declare const $dataToText: (data: string | Uint8Array, encoding: IPC_DATA_ENCODING) => string;
