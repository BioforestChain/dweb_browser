import { IpcMessage, IPC_MESSAGE_TYPE } from "./const.js";
export declare class IpcStreamAbort extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_ABORT> {
    readonly stream_id: string;
    constructor(stream_id: string);
}
