"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NativeIpc = void 0;
const ipc_1 = require("./ipc");
class NativeIpc extends ipc_1.Ipc {
    constructor(port) {
        super();
        this.port = port;
        this._cbs = new Set();
        port.addEventListener("message", (event) => {
            const { req_id, method, url, body, headers } = event.data;
            const request = new ipc_1.IpcRequest(req_id, method, url, body, headers);
            for (const cb of this._cbs) {
                cb(request);
            }
        });
    }
    postMessage(request) {
        this.port.postMessage(JSON.stringify(request));
    }
    onMessage(cb) {
        this._cbs.add(cb);
        return () => this._cbs.delete(cb);
    }
}
exports.NativeIpc = NativeIpc;
