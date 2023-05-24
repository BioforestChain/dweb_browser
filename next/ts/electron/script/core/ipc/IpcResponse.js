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
var _IpcResponse_ipcHeaders;
Object.defineProperty(exports, "__esModule", { value: true });
exports.IpcResMessage = exports.IpcResponse = void 0;
const lodash_1 = require("lodash");
const binaryHelper_js_1 = require("../../helper/binaryHelper.js");
const const_js_1 = require("./const.js");
const IpcBodySender_js_1 = require("./IpcBodySender.js");
const IpcHeaders_js_1 = require("./IpcHeaders.js");
class IpcResponse extends const_js_1.IpcMessage {
    constructor(req_id, statusCode, headers, body, ipc) {
        super(const_js_1.IPC_MESSAGE_TYPE.RESPONSE);
        Object.defineProperty(this, "req_id", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: req_id
        });
        Object.defineProperty(this, "statusCode", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: statusCode
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
        _IpcResponse_ipcHeaders.set(this, void 0);
        Object.defineProperty(this, "ipcResMessage", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, lodash_1.once)(() => new IpcResMessage(this.req_id, this.statusCode, this.headers.toJSON(), this.body.metaBody))
        });
        if (body instanceof IpcBodySender_js_1.IpcBodySender) {
            IpcBodySender_js_1.IpcBodySender.$usableByIpc(ipc, body);
        }
    }
    get ipcHeaders() {
        return (__classPrivateFieldSet(this, _IpcResponse_ipcHeaders, __classPrivateFieldGet(this, _IpcResponse_ipcHeaders, "f") ?? new IpcHeaders_js_1.IpcHeaders(this.headers), "f"));
    }
    toResponse(url) {
        const body = this.body.raw;
        if (body instanceof Uint8Array) {
            this.headers.init("Content-Length", body.length + "");
        }
        const response = new Response(body, {
            headers: this.headers,
            status: this.statusCode,
        });
        if (url) {
            Object.defineProperty(response, "url", {
                value: url,
                enumerable: true,
                configurable: true,
                writable: false,
            });
        }
        return response;
    }
    /** 将 response 对象进行转码变成 ipcResponse */
    static async fromResponse(req_id, response, ipc, 
    /// 如果有 content-length，说明大小是明确的，不要走流，直接传输就好，减少 IPC 的触发次数. TODO 需要注意大小是否过大，过大的话还是要分片传输。不过这种大二进制情况下一般是请求文件，应该直接使用句柄转发
    asBinary = false // response.headers.get("content-length") !== null
    ) {
        if (response.bodyUsed) {
            throw new Error("body used");
        }
        let ipcBody;
        if (asBinary || response.body == undefined) {
            ipcBody = IpcBodySender_js_1.IpcBodySender.from((0, binaryHelper_js_1.binaryToU8a)(await response.arrayBuffer()), ipc);
        }
        else {
            (0, IpcBodySender_js_1.setStreamId)(response.body, response.url);
            ipcBody = IpcBodySender_js_1.IpcBodySender.from(response.body, ipc);
        }
        return new IpcResponse(req_id, response.status, new IpcHeaders_js_1.IpcHeaders(response.headers), ipcBody, ipc);
    }
    static fromJson(req_id, statusCode, headers = new IpcHeaders_js_1.IpcHeaders(), jsonable, ipc) {
        headers.init("Content-Type", "application/json");
        return this.fromText(req_id, statusCode, headers, JSON.stringify(jsonable), ipc);
    }
    static fromText(req_id, statusCode, headers = new IpcHeaders_js_1.IpcHeaders(), text, ipc) {
        headers.init("Content-Type", "text/plain");
        // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
        return new IpcResponse(req_id, statusCode, headers, IpcBodySender_js_1.IpcBodySender.from(text, ipc), ipc);
    }
    static fromBinary(req_id, statusCode, headers = new IpcHeaders_js_1.IpcHeaders(), binary, ipc) {
        headers.init("Content-Type", "application/octet-stream");
        headers.init("Content-Length", binary.byteLength + "");
        return new IpcResponse(req_id, statusCode, headers, IpcBodySender_js_1.IpcBodySender.from((0, binaryHelper_js_1.binaryToU8a)(binary), ipc), ipc);
    }
    static fromStream(req_id, statusCode, headers = new IpcHeaders_js_1.IpcHeaders(), stream, ipc) {
        headers.init("Content-Type", "application/octet-stream");
        const ipcResponse = new IpcResponse(req_id, statusCode, headers, IpcBodySender_js_1.IpcBodySender.from(stream, ipc), ipc);
        return ipcResponse;
    }
    toJSON() {
        return this.ipcResMessage();
    }
}
exports.IpcResponse = IpcResponse;
_IpcResponse_ipcHeaders = new WeakMap();
class IpcResMessage extends const_js_1.IpcMessage {
    constructor(req_id, statusCode, headers, metaBody) {
        super(const_js_1.IPC_MESSAGE_TYPE.RESPONSE);
        Object.defineProperty(this, "req_id", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: req_id
        });
        Object.defineProperty(this, "statusCode", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: statusCode
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
exports.IpcResMessage = IpcResMessage;
