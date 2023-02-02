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
var _IpcRequest_parsed_url, _IpcResponse_body;
Object.defineProperty(exports, "__esModule", { value: true });
exports.Ipc = exports.IpcResponse = exports.IpcStreamEnd = exports.IpcStream = exports.IpcRequest = void 0;
const helper_cjs_1 = require("./helper.cjs");
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
class IpcStream {
    constructor(stream_id, data) {
        this.stream_id = stream_id;
        this.data = data;
        this.type = 2 /* IPC_DATA_TYPE.STREAM */;
    }
}
exports.IpcStream = IpcStream;
class IpcStreamEnd {
    constructor(stream_id) {
        this.stream_id = stream_id;
        this.type = 3 /* IPC_DATA_TYPE.STREAM_END */;
    }
}
exports.IpcStreamEnd = IpcStreamEnd;
class IpcResponse {
    constructor(req_id, statusCode, rawBody, headers, ipc) {
        this.req_id = req_id;
        this.statusCode = statusCode;
        this.rawBody = rawBody;
        this.headers = headers;
        this.type = 1 /* IPC_DATA_TYPE.RESPONSE */;
        _IpcResponse_body.set(this, void 0);
        let body;
        const raw_body_type = rawBody[0];
        const encoding = raw_body_type & 4 /* IPC_RESPONSE_RAW_BODY_TYPE.BASE64 */ ? "base64" : "utf8";
        if (raw_body_type === 8 /* IPC_RESPONSE_RAW_BODY_TYPE.STREAM_ID */) {
            if (ipc == null) {
                throw new Error(`miss ipc when ipc-response has stream-body`);
            }
            const stream_id = rawBody[1];
            body = new ReadableStream({
                start(controller) {
                    const off = ipc.onMessage((message) => {
                        if ("stream_id" in message && message.stream_id === stream_id) {
                            if (message.type === 2 /* IPC_DATA_TYPE.STREAM */) {
                                controller.enqueue((0, helper_cjs_1.simpleEncoder)(message.data, encoding));
                            }
                            else if (message.type === 3 /* IPC_DATA_TYPE.STREAM_END */) {
                                controller.close();
                                off();
                            }
                        }
                    });
                },
            });
        }
        else {
            body = (0, helper_cjs_1.simpleEncoder)(rawBody[1], encoding);
        }
        __classPrivateFieldSet(this, _IpcResponse_body, body, "f");
    }
    get body() {
        return __classPrivateFieldGet(this, _IpcResponse_body, "f");
    }
    asResponse() {
        const response = new Response(this.body, {
            headers: this.headers,
            status: this.statusCode,
        });
        response[IpcResponse.ORIGIN] = this;
        return response;
    }
    /** 将 response 对象进行转码变成 ipcResponse */
    static async formResponse(req_id, response, ipc) {
        let ipcResponse = response[IpcResponse.ORIGIN];
        if (ipcResponse === undefined) {
            const contentLength = response.headers.get("Content-Length") ?? 0;
            /// 6kb 为分水岭，超过 6kb 就改用流传输，或者如果原本就是流对象
            /// TODO 这里是否使用流模式，应该引入更加只能的判断方式，比如先读取一部分数据，如果能读取完成，那么就直接吧 end 符号一起带过去
            if (contentLength > 6144 || response.body) {
                const stream_id = `${req_id}/${contentLength}`;
                ipcResponse = new IpcResponse(req_id, response.status, [12 /* IPC_RESPONSE_RAW_BODY_TYPE.BASE64_STREAM_ID */, stream_id], (0, helper_cjs_1.headersToRecord)(response.headers));
                (async (stream) => {
                    const reader = stream.getReader();
                    while (true) {
                        const item = await reader.read();
                        if (item.done) {
                            ipc.postMessage(new IpcStreamEnd(stream_id));
                        }
                        else {
                            ipc.postMessage(new IpcStream(stream_id, (0, helper_cjs_1.simpleDecoder)(item.value, "base64")));
                        }
                    }
                })(response.body ?? (await response.blob()).stream());
            }
            else {
                ipcResponse = new IpcResponse(req_id, response.status, [
                    4 /* IPC_RESPONSE_RAW_BODY_TYPE.BASE64 */,
                    (0, helper_cjs_1.simpleDecoder)(await response.arrayBuffer(), "base64"),
                ], (0, helper_cjs_1.headersToRecord)(response.headers));
            }
            response[IpcResponse.ORIGIN] = ipcResponse;
        }
        return ipcResponse;
    }
    static fromJson(req_id, statusCode, json, headers = {}) {
        headers["Content-Type"] = "application/json";
        return new IpcResponse(req_id, statusCode, [2 /* IPC_RESPONSE_RAW_BODY_TYPE.TEXT */, JSON.stringify(json)], headers);
    }
    static fromText(req_id, statusCode, json, headers = {}) {
        headers["Content-Type"] = "text/plain";
        return new IpcResponse(req_id, statusCode, [2 /* IPC_RESPONSE_RAW_BODY_TYPE.TEXT */, JSON.stringify(json)], headers);
    }
    static fromBinary(req_id, statusCode, binary, headers = {}) {
        headers["Content-Type"] = "application/octet-stream";
        return new IpcResponse(req_id, statusCode, [4 /* IPC_RESPONSE_RAW_BODY_TYPE.BASE64 */, (0, helper_cjs_1.simpleDecoder)(binary, "base64")], headers);
    }
}
exports.IpcResponse = IpcResponse;
_IpcResponse_body = new WeakMap();
IpcResponse.ORIGIN = Symbol.for("origin-ipc-response");
let ipc_uid_acc = 0;
class Ipc {
    constructor() {
        this.uid = ipc_uid_acc++;
    }
}
exports.Ipc = Ipc;
