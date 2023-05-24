import { IpcMessage, IPC_DATA_ENCODING, IPC_MESSAGE_TYPE } from "./const.js";
export declare class IpcStreamData extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_DATA> {
    readonly stream_id: string;
    readonly data: string | Uint8Array;
    readonly encoding: IPC_DATA_ENCODING;
    constructor(stream_id: string, data: string | Uint8Array, encoding: IPC_DATA_ENCODING);
    static fromBase64(stream_id: string, data: Uint8Array): IpcStreamData;
    static fromBinary(stream_id: string, data: Uint8Array): IpcStreamData;
    static fromUtf8(stream_id: string, data: Uint8Array): IpcStreamData;
    get binary(): Uint8Array;
    get text(): string;
    get jsonAble(): IpcStreamData;
    toJSON(): {
        stream_id: string;
        data: string | Uint8Array;
        encoding: IPC_DATA_ENCODING;
        type: IPC_MESSAGE_TYPE.STREAM_DATA;
    };
}
