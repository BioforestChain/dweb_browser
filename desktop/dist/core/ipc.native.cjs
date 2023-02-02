"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NativeIpc = exports.$messageToIpcMessage = void 0;
const ipc_cjs_1 = require("./ipc.cjs");
const $messageToIpcMessage = (data) => {
    let message;
    /*  if (data instanceof IpcRequest || data instanceof IpcResponse) {
        message = data;
     } else  */ if (data === "close") {
        message = data;
    }
    else if (data.type === 0 /* IPC_DATA_TYPE.REQUEST */) {
        message = new ipc_cjs_1.IpcRequest(data.req_id, data.method, data.url, data.body, data.headers);
    }
    else if (data.type === 1 /* IPC_DATA_TYPE.RESPONSE */) {
        message = new ipc_cjs_1.IpcResponse(data.req_id, data.statusCode, data.body, data.headers);
    }
    return message;
};
exports.$messageToIpcMessage = $messageToIpcMessage;
class NativeIpc extends ipc_cjs_1.Ipc {
    constructor(port, module) {
        super();
        this.port = port;
        this.module = module;
        this._cbs = new Set();
        this._closed = false;
        this._onclose_cbs = new Set();
        port.addEventListener("message", (event) => {
            const message = (0, exports.$messageToIpcMessage)(event.data);
            if (message === undefined) {
                return;
            }
            if (message === "close") {
                this.close();
                return;
            }
            /// ipc-message
            for (const cb of this._cbs) {
                cb(message);
            }
        });
        port.start();
    }
    postMessage(message) {
        if (this._closed) {
            return;
        }
        this.port.postMessage(message);
    }
    onMessage(cb) {
        this._cbs.add(cb);
        return () => this._cbs.delete(cb);
    }
    close() {
        if (this._closed) {
            return;
        }
        this._closed = true;
        this.port.postMessage("close");
        this.port.close();
        for (const cb of this._onclose_cbs) {
            cb();
        }
    }
    onClose(cb) {
        this._onclose_cbs.add(cb);
        return () => this._onclose_cbs.delete(cb);
    }
}
exports.NativeIpc = NativeIpc;
