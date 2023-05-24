import { IpcMessage, IPC_MESSAGE_TYPE } from "./const.js";
export class IpcStreamAbort extends IpcMessage {
    constructor(stream_id) {
        super(IPC_MESSAGE_TYPE.STREAM_ABORT);
        Object.defineProperty(this, "stream_id", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: stream_id
        });
    }
}
