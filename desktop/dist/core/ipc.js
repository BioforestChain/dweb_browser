"use strict";
var __classPrivateFieldGet = (this && this.__classPrivateFieldGet) || function (receiver, state, kind, f) {
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a getter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot read private member from an object whose class did not declare it");
    return kind === "m" ? f : kind === "a" ? f.call(receiver) : f ? f.value : state.get(receiver);
};
var __classPrivateFieldSet = (this && this.__classPrivateFieldSet) || function (receiver, state, value, kind, f) {
    if (kind === "m") throw new TypeError("Private method is not writable");
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a setter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot write private member to an object whose class did not declare it");
    return (kind === "a" ? f.call(receiver, value) : f ? f.value = value : state.set(receiver, value)), value;
};
var _IpcRequest_parsed_url;
Object.defineProperty(exports, "__esModule", { value: true });
exports.Ipc = exports.IpcResponse = exports.IpcRequest = void 0;
class IpcRequest {
    constructor(req_id, method, url, body, headers) {
        this.req_id = req_id;
        this.method = method;
        this.url = url;
        this.body = body;
        this.headers = headers;
        this.type = 0 /* IPC_DATA_TYPE.REQUEST */;
        _IpcRequest_parsed_url.set(this, void 0);
    }
    get parsed_url() {
        return (__classPrivateFieldSet(this, _IpcRequest_parsed_url, __classPrivateFieldGet(this, _IpcRequest_parsed_url, "f") ?? new URL(this.url), "f"));
    }
}
exports.IpcRequest = IpcRequest;
_IpcRequest_parsed_url = new WeakMap();
class IpcResponse {
    constructor(req_id, statusCode, body, headers) {
        this.req_id = req_id;
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
        this.type = 1 /* IPC_DATA_TYPE.RESPONSE */;
    }
}
exports.IpcResponse = IpcResponse;
class Ipc {
}
exports.Ipc = Ipc;
