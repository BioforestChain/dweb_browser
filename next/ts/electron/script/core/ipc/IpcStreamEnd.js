"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.IpcStreamEnd = void 0;
const const_js_1 = require("./const.js");
class IpcStreamEnd extends const_js_1.IpcMessage {
    constructor(stream_id) {
        super(const_js_1.IPC_MESSAGE_TYPE.STREAM_END);
        Object.defineProperty(this, "stream_id", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: stream_id
        });
    }
}
exports.IpcStreamEnd = IpcStreamEnd;
