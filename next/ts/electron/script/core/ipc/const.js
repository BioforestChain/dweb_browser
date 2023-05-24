"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.$dataToText = exports.$dataToBinary = exports.IpcMessage = exports.IPC_ROLE = exports.IPC_DATA_ENCODING = exports.IPC_MESSAGE_TYPE = exports.toIpcMethod = exports.IPC_METHOD = void 0;
const encoding_js_1 = require("../../helper/encoding.js");
var IPC_METHOD;
(function (IPC_METHOD) {
    IPC_METHOD["GET"] = "GET";
    IPC_METHOD["POST"] = "POST";
    IPC_METHOD["PUT"] = "PUT";
    IPC_METHOD["DELETE"] = "DELETE";
    IPC_METHOD["OPTIONS"] = "OPTIONS";
    IPC_METHOD["TRACE"] = "TRACE";
    IPC_METHOD["PATCH"] = "PATCH";
    IPC_METHOD["PURGE"] = "PURGE";
    IPC_METHOD["HEAD"] = "HEAD";
})(IPC_METHOD = exports.IPC_METHOD || (exports.IPC_METHOD = {}));
const toIpcMethod = (method) => {
    if (method == null) {
        return IPC_METHOD.GET;
    }
    switch (method.toUpperCase()) {
        case IPC_METHOD.GET: {
            return IPC_METHOD.GET;
        }
        case IPC_METHOD.POST: {
            return IPC_METHOD.POST;
        }
        case IPC_METHOD.PUT: {
            return IPC_METHOD.PUT;
        }
        case IPC_METHOD.DELETE: {
            return IPC_METHOD.DELETE;
        }
        case IPC_METHOD.OPTIONS: {
            return IPC_METHOD.OPTIONS;
        }
        case IPC_METHOD.TRACE: {
            return IPC_METHOD.TRACE;
        }
        case IPC_METHOD.PATCH: {
            return IPC_METHOD.PATCH;
        }
        case IPC_METHOD.PURGE: {
            return IPC_METHOD.PURGE;
        }
        case IPC_METHOD.HEAD: {
            return IPC_METHOD.HEAD;
        }
    }
    throw new Error(`invalid method: ${method}`);
};
exports.toIpcMethod = toIpcMethod;
var IPC_MESSAGE_TYPE;
(function (IPC_MESSAGE_TYPE) {
    // /** 特殊位：结束符 */
    // END = 1,
    /** 类型：请求 */
    IPC_MESSAGE_TYPE[IPC_MESSAGE_TYPE["REQUEST"] = 0] = "REQUEST";
    /** 类型：相应 */
    IPC_MESSAGE_TYPE[IPC_MESSAGE_TYPE["RESPONSE"] = 1] = "RESPONSE";
    /** 类型：流数据，发送方 */
    IPC_MESSAGE_TYPE[IPC_MESSAGE_TYPE["STREAM_DATA"] = 2] = "STREAM_DATA";
    /** 类型：流拉取，请求方
     * 发送方一旦收到该指令，就可以持续发送数据
     * 该指令中可以携带一些“限流协议信息”，如果违背该协议，请求方可能会断开连接
     */
    IPC_MESSAGE_TYPE[IPC_MESSAGE_TYPE["STREAM_PULLING"] = 3] = "STREAM_PULLING";
    /** 类型：流暂停，请求方
     * 发送方一旦收到该指令，就应当停止基本的数据发送
     * 该指令中可以携带一些“保险协议信息”，描述仍然允许发送的一些数据类型、发送频率等。如果违背该协议，请求方可以会断开连接
     */
    IPC_MESSAGE_TYPE[IPC_MESSAGE_TYPE["STREAM_PAUSED"] = 4] = "STREAM_PAUSED";
    /** 类型：流关闭，发送方
     * 可能是发送完成了，也有可能是被中断了
     */
    IPC_MESSAGE_TYPE[IPC_MESSAGE_TYPE["STREAM_END"] = 5] = "STREAM_END";
    /** 类型：流中断，请求方 */
    IPC_MESSAGE_TYPE[IPC_MESSAGE_TYPE["STREAM_ABORT"] = 6] = "STREAM_ABORT";
    /** 类型：事件 */
    IPC_MESSAGE_TYPE[IPC_MESSAGE_TYPE["EVENT"] = 7] = "EVENT";
})(IPC_MESSAGE_TYPE = exports.IPC_MESSAGE_TYPE || (exports.IPC_MESSAGE_TYPE = {}));
/**
 * 数据编码格式
 */
var IPC_DATA_ENCODING;
(function (IPC_DATA_ENCODING) {
    /** 文本 json html 等 */
    IPC_DATA_ENCODING[IPC_DATA_ENCODING["UTF8"] = 2] = "UTF8";
    /** 使用文本表示的二进制 */
    IPC_DATA_ENCODING[IPC_DATA_ENCODING["BASE64"] = 4] = "BASE64";
    /** 二进制 */
    IPC_DATA_ENCODING[IPC_DATA_ENCODING["BINARY"] = 8] = "BINARY";
})(IPC_DATA_ENCODING = exports.IPC_DATA_ENCODING || (exports.IPC_DATA_ENCODING = {}));
var IPC_ROLE;
(function (IPC_ROLE) {
    IPC_ROLE["SERVER"] = "server";
    IPC_ROLE["CLIENT"] = "client";
})(IPC_ROLE = exports.IPC_ROLE || (exports.IPC_ROLE = {}));
class IpcMessage {
    constructor(type) {
        Object.defineProperty(this, "type", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: type
        });
    }
}
exports.IpcMessage = IpcMessage;
const $dataToBinary = (data, encoding) => {
    switch (encoding) {
        case IPC_DATA_ENCODING.BINARY: {
            return data;
        }
        case IPC_DATA_ENCODING.BASE64: {
            return (0, encoding_js_1.simpleEncoder)(data, "base64");
        }
        case IPC_DATA_ENCODING.UTF8: {
            return (0, encoding_js_1.simpleEncoder)(data, "utf8");
        }
    }
    throw new Error(`unknown encoding: ${encoding}`);
};
exports.$dataToBinary = $dataToBinary;
const $dataToText = (data, encoding) => {
    switch (encoding) {
        case IPC_DATA_ENCODING.BINARY: {
            return (0, encoding_js_1.simpleDecoder)(data, "utf8");
        }
        case IPC_DATA_ENCODING.BASE64: {
            return (0, encoding_js_1.simpleDecoder)((0, encoding_js_1.simpleEncoder)(data, "base64"), "utf8");
        }
        case IPC_DATA_ENCODING.UTF8: {
            return data;
        }
    }
    throw new Error(`unknown encoding: ${encoding}`);
};
exports.$dataToText = $dataToText;
