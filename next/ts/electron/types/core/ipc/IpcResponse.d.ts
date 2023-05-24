import { $Binary } from "../../helper/binaryHelper.js";
import { IpcMessage, IPC_MESSAGE_TYPE } from "./const.js";
import type { Ipc } from "./ipc.js";
import type { IpcBody } from "./IpcBody.js";
import { IpcHeaders } from "./IpcHeaders.js";
import type { MetaBody } from "./MetaBody.js";
export declare class IpcResponse extends IpcMessage<IPC_MESSAGE_TYPE.RESPONSE> {
    #private;
    readonly req_id: number;
    readonly statusCode: number;
    readonly headers: IpcHeaders;
    readonly body: IpcBody;
    readonly ipc: Ipc;
    constructor(req_id: number, statusCode: number, headers: IpcHeaders, body: IpcBody, ipc: Ipc);
    get ipcHeaders(): IpcHeaders;
    toResponse(url?: string): Response;
    /** 将 response 对象进行转码变成 ipcResponse */
    static fromResponse(req_id: number, response: Response, ipc: Ipc, asBinary?: boolean): Promise<IpcResponse>;
    static fromJson(req_id: number, statusCode: number, headers: IpcHeaders | undefined, jsonable: unknown, ipc: Ipc): IpcResponse;
    static fromText(req_id: number, statusCode: number, headers: IpcHeaders | undefined, text: string, ipc: Ipc): IpcResponse;
    static fromBinary(req_id: number, statusCode: number, headers: IpcHeaders | undefined, binary: $Binary, ipc: Ipc): IpcResponse;
    static fromStream(req_id: number, statusCode: number, headers: IpcHeaders | undefined, stream: ReadableStream<Uint8Array>, ipc: Ipc): IpcResponse;
    readonly ipcResMessage: any;
    toJSON(): any;
}
export declare class IpcResMessage extends IpcMessage<IPC_MESSAGE_TYPE.RESPONSE> {
    readonly req_id: number;
    readonly statusCode: number;
    readonly headers: Record<string, string>;
    readonly metaBody: MetaBody;
    constructor(req_id: number, statusCode: number, headers: Record<string, string>, metaBody: MetaBody);
}
