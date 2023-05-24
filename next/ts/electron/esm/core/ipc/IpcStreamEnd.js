import { IpcMessage, IPC_MESSAGE_TYPE } from "./const.js";
export class IpcStreamEnd extends IpcMessage {
    constructor(stream_id) {
        super(IPC_MESSAGE_TYPE.STREAM_END);
        Object.defineProperty(this, "stream_id", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: stream_id
        });
    }
}
