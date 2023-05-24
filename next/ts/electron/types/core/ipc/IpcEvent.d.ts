import { IpcMessage, IPC_DATA_ENCODING, IPC_MESSAGE_TYPE } from "./const.js";
export declare class IpcEvent extends IpcMessage<IPC_MESSAGE_TYPE.EVENT> {
    readonly name: string;
    readonly data: string | Uint8Array;
    readonly encoding: IPC_DATA_ENCODING;
    constructor(name: string, data: string | Uint8Array, encoding: IPC_DATA_ENCODING);
    static fromBase64(name: string, data: Uint8Array): IpcEvent;
    static fromBinary(name: string, data: Uint8Array): IpcEvent;
    static fromUtf8(name: string, data: Uint8Array): IpcEvent;
    static fromText(name: string, data: string): IpcEvent;
    get binary(): Uint8Array;
    get text(): string;
    get jsonAble(): IpcEvent;
    toJSON(): {
        name: string;
        data: string | Uint8Array;
        encoding: IPC_DATA_ENCODING;
        type: IPC_MESSAGE_TYPE.EVENT;
    };
}
