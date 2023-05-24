import { $Binary } from "../../helper/binaryHelper.js";
import { IpcMessage, IPC_MESSAGE_TYPE, IPC_METHOD } from "./const.js";
import type { Ipc } from "./ipc.js";
import type { IpcBody } from "./IpcBody.js";
import { IpcHeaders } from "./IpcHeaders.js";
import type { MetaBody } from "./MetaBody.js";
export declare class IpcRequest extends IpcMessage<IPC_MESSAGE_TYPE.REQUEST> {
    #private;
    readonly req_id: number;
    readonly url: string;
    readonly method: IPC_METHOD;
    readonly headers: IpcHeaders;
    readonly body: IpcBody;
    readonly ipc: Ipc;
    constructor(req_id: number, url: string, method: IPC_METHOD, headers: IpcHeaders, body: IpcBody, ipc: Ipc);
    get parsed_url(): URL;
    static fromText(req_id: number, url: string, method: IPC_METHOD | undefined, headers: IpcHeaders | undefined, text: string, ipc: Ipc): IpcRequest;
    static fromBinary(req_id: number, url: string, method: IPC_METHOD | undefined, headers: IpcHeaders | undefined, binary: $Binary, ipc: Ipc): IpcRequest;
    static fromStream(req_id: number, url: string, method: IPC_METHOD | undefined, headers: IpcHeaders | undefined, stream: ReadableStream<Uint8Array>, ipc: Ipc): IpcRequest;
    static fromRequest(req_id: number, ipc: Ipc, url: string, init?: {
        method?: string;
        body?: string | Uint8Array | ReadableStream<Uint8Array>;
        headers?: IpcHeaders | HeadersInit;
    }): IpcRequest;
    toRequest(): Request;
    readonly ipcReqMessage: any;
    toJSON(): any;
}
export declare class IpcReqMessage extends IpcMessage<IPC_MESSAGE_TYPE.REQUEST> {
    readonly req_id: number;
    readonly method: IPC_METHOD;
    readonly url: string;
    readonly headers: Record<string, string>;
    readonly metaBody: MetaBody;
    constructor(req_id: number, method: IPC_METHOD, url: string, headers: Record<string, string>, metaBody: MetaBody);
}
