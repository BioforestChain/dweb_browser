"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NativeIpc = exports.$messageToIpcMessage = void 0;
const ipc_cjs_1 = require("./ipc.cjs");
const $messageToIpcMessage = (data, ipc) => {
    let message;
    if (data === "close") {
        message = data;
    }
    else if (data.type === 0 /* IPC_DATA_TYPE.REQUEST */) {
        message = new ipc_cjs_1.IpcRequest(data.req_id, data.method, data.url, data.body, data.headers);
    }
    else if (data.type === 1 /* IPC_DATA_TYPE.RESPONSE */) {
        message = new ipc_cjs_1.IpcResponse(data.req_id, data.statusCode, data.rawBody, data.headers, ipc);
    }
    else if (data.type === 2 /* IPC_DATA_TYPE.STREAM */) {
        message = new ipc_cjs_1.IpcStream(data.stream_id, data.data);
    }
    else if (data.type === 3 /* IPC_DATA_TYPE.STREAM_END */) {
        message = new ipc_cjs_1.IpcStreamEnd(data.stream_id);
    }
    return message;
};
exports.$messageToIpcMessage = $messageToIpcMessage;
class NativeIpc extends ipc_cjs_1.Ipc {
    constructor(port, module, role) {
        super();
        this.port = port;
        this.module = module;
        this.role = role;
        this._cbs = new Set();
        this._closed = false;
        this._onclose_cbs = new Set();
        port.addEventListener("message", (event) => {
            const message = (0, exports.$messageToIpcMessage)(event.data, this);
            if (message === undefined) {
                return;
            }
            if (message === "close") {
                this.close();
                return;
            }
            if (message.type === 2 /* IPC_DATA_TYPE.STREAM */ ||
                message.type === 3 /* IPC_DATA_TYPE.STREAM_END */) {
                return;
            }
            /// ipc-message
            for (const cb of this._cbs) {
                cb(message, this);
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
