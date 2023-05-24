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
exports.IpcReqMessage = exports.IpcRequest = void 0;
const lodash_1 = require("lodash");
const binaryHelper_js_1 = require("../../helper/binaryHelper.js");
const urlHelper_js_1 = require("../../helper/urlHelper.js");
const const_js_1 = require("./const.js");
const IpcBodySender_js_1 = require("./IpcBodySender.js");
const IpcHeaders_js_1 = require("./IpcHeaders.js");
class IpcRequest extends const_js_1.IpcMessage {
    constructor(req_id, url, method, headers, body, ipc) {
        super(const_js_1.IPC_MESSAGE_TYPE.REQUEST);
        Object.defineProperty(this, "req_id", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: req_id
        });
        Object.defineProperty(this, "url", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: url
        });
        Object.defineProperty(this, "method", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: method
        });
        Object.defineProperty(this, "headers", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: headers
        });
        Object.defineProperty(this, "body", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: body
        });
        Object.defineProperty(this, "ipc", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ipc
        });
        _IpcRequest_parsed_url.set(this, void 0);
        Object.defineProperty(this, "ipcReqMessage", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, lodash_1.once)(() => new IpcReqMessage(this.req_id, this.method, this.url, this.headers.toJSON(), this.body.metaBody))
        });
        if (body instanceof IpcBodySender_js_1.IpcBodySender) {
            IpcBodySender_js_1.IpcBodySender.$usableByIpc(ipc, body);
        }
    }
    get parsed_url() {
        return (__classPrivateFieldSet(this, _IpcRequest_parsed_url, __classPrivateFieldGet(this, _IpcRequest_parsed_url, "f") ?? (0, urlHelper_js_1.parseUrl)(this.url), "f"));
    }
    static fromText(req_id, url, method = const_js_1.IPC_METHOD.GET, headers = new IpcHeaders_js_1.IpcHeaders(), text, ipc) {
        // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
        return new IpcRequest(req_id, url, method, headers, IpcBodySender_js_1.IpcBodySender.from(text, ipc), ipc);
    }
    static fromBinary(req_id, url, method = const_js_1.IPC_METHOD.GET, headers = new IpcHeaders_js_1.IpcHeaders(), binary, ipc) {
        headers.init("Content-Type", "application/octet-stream");
        headers.init("Content-Length", binary.byteLength + "");
        return new IpcRequest(req_id, url, method, headers, IpcBodySender_js_1.IpcBodySender.from((0, binaryHelper_js_1.binaryToU8a)(binary), ipc), ipc);
    }
    static fromStream(req_id, url, method = const_js_1.IPC_METHOD.GET, headers = new IpcHeaders_js_1.IpcHeaders(), stream, ipc) {
        headers.init("Content-Type", "application/octet-stream");
        return new IpcRequest(req_id, url, method, headers, IpcBodySender_js_1.IpcBodySender.from(stream, ipc), ipc);
    }
    static fromRequest(req_id, ipc, url, init = {}) {
        const method = (0, const_js_1.toIpcMethod)(init.method);
        const headers = init.headers instanceof IpcHeaders_js_1.IpcHeaders
            ? init.headers
            : new IpcHeaders_js_1.IpcHeaders(init.headers);
        let ipcBody;
        if ((0, binaryHelper_js_1.isBinary)(init.body)) {
            ipcBody = IpcBodySender_js_1.IpcBodySender.from(init.body, ipc);
        }
        else if (init.body instanceof ReadableStream) {
            ipcBody = IpcBodySender_js_1.IpcBodySender.from(init.body, ipc);
        }
        else {
            ipcBody = IpcBodySender_js_1.IpcBodySender.from(init.body ?? "", ipc);
        }
        return new IpcRequest(req_id, url, method, headers, ipcBody, ipc);
    }
    toRequest() {
        const { method } = this;
        let body;
        if ((method === const_js_1.IPC_METHOD.GET || method === const_js_1.IPC_METHOD.HEAD) === false) {
            body = this.body.raw;
        }
        const init = {
            method,
            headers: this.headers,
            body,
        };
        if (body) {
            Reflect.set(init, "duplex", "half");
        }
        return new Request(this.url, init);
    }
    toJSON() {
        const { method } = this;
        let body;
        if ((method === const_js_1.IPC_METHOD.GET || method === const_js_1.IPC_METHOD.HEAD) === false) {
            body = this.body.raw;
            return new IpcReqMessage(this.req_id, this.method, this.url, this.headers.toJSON(), this.body.metaBody);
        }
        return this.ipcReqMessage();
    }
}
exports.IpcRequest = IpcRequest;
_IpcRequest_parsed_url = new WeakMap();
class IpcReqMessage extends const_js_1.IpcMessage {
    constructor(req_id, method, url, headers, metaBody) {
        super(const_js_1.IPC_MESSAGE_TYPE.REQUEST);
        Object.defineProperty(this, "req_id", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: req_id
        });
        Object.defineProperty(this, "method", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: method
        });
        Object.defineProperty(this, "url", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: url
        });
        Object.defineProperty(this, "headers", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: headers
        });
        Object.defineProperty(this, "metaBody", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: metaBody
        });
    }
}
exports.IpcReqMessage = IpcReqMessage;
