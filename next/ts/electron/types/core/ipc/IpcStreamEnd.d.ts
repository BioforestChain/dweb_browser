import { IpcMessage, IPC_MESSAGE_TYPE } from "./const.js";
export declare class IpcStreamEnd extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_END> {
    readonly stream_id: string;
    constructor(stream_id: string);
}
