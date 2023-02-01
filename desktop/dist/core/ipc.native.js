"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NativeIpc = void 0;
const ipc_1 = require("./ipc");
class NativeIpc extends ipc_1.Ipc {
    constructor(port) {
        super();
        this.port = port;
        this._cbs = new Set();
        this._closed = false;
        this._onclose_cbs = new Set();
        port.addEventListener("message", (event) => {
            const data = event.data; // | IpcResponse // | IpcRequest
            let message;
            /*  if (data instanceof IpcRequest || data instanceof IpcResponse) {
               message = data;
            } else  */ if (data === "close") {
                this.close();
            }
            else if (data.type === 0 /* IPC_DATA_TYPE.REQUEST */) {
                message = new ipc_1.IpcRequest(data.req_id, data.method, data.url, data.body, data.headers);
            }
            else if (data.type === 1 /* IPC_DATA_TYPE.RESPONSE */) {
                message = new ipc_1.IpcResponse(data.req_id, data.statusCode, data.body, data.headers);
            }
            if (message === undefined) {
                return;
            }
            for (const cb of this._cbs) {
                cb(message);
            }
        });
        port.start();
    }
    postMessage(request) {
        if (this._closed) {
            return;
        }
        this.port.postMessage(request);
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
