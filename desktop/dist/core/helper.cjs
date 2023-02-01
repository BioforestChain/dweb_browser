"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.normalizeFetchArgs = exports.readRequestAsIpcRequest = exports.openNwWindow = exports.PromiseOut = exports.$serializeResultToResponse = exports.$deserializeRequestToParams = exports.$typeNameParser = void 0;
const ipc_cjs_1 = require("./ipc.cjs");
const $typeNameParser = (key, typeName2, value) => {
    let param;
    if (value === null) {
        if (typeName2.endsWith("?")) {
            throw new Error(`param type error: '${key}'.`);
        }
        else {
            param = undefined;
        }
    }
    else {
        const typeName1 = (typeName2.endsWith("?") ? typeName2.slice(0, -1) : typeName2);
        switch (typeName1) {
            case "number": {
                param = +value;
                break;
            }
            case "boolean": {
                param = value === "" ? false : Boolean(value.toLowerCase());
                break;
            }
            case "mmid": {
                if (value.endsWith(".dweb") === false) {
                    throw new Error(`param mmid type error: '${key}':'${value}'`);
                }
                param = value;
                break;
            }
            case "string": {
                param = value;
                break;
            }
            default:
                param = void 0;
        }
    }
    return param;
};
exports.$typeNameParser = $typeNameParser;
const $deserializeRequestToParams = (schema) => {
    return (request) => {
        const url = request.parsed_url;
        const params = {};
        for (const [key, typeName2] of Object.entries(schema)) {
            params[key] = (0, exports.$typeNameParser)(key, typeName2, url.searchParams.get(key));
        }
        return params;
    };
};
exports.$deserializeRequestToParams = $deserializeRequestToParams;
/**
 * @TODO 实现模式匹配
 */
const $serializeResultToResponse = (schema) => {
    return (request, result) => {
        return new ipc_cjs_1.IpcResponse(request.req_id, 200, JSON.stringify(result), {
            "Content-Type": "application/json",
        });
    };
};
exports.$serializeResultToResponse = $serializeResultToResponse;
class PromiseOut {
    constructor() {
        this.promise = new Promise((resolve, reject) => {
            this.resolve = resolve;
            this.reject = reject;
        });
    }
}
exports.PromiseOut = PromiseOut;
const openNwWindow = (url, options) => {
    return new Promise((resolve) => {
        nw.Window.open(url, options, resolve);
    });
};
exports.openNwWindow = openNwWindow;
/** 将 request 参数解构 成 ipcRequest 的参数 */
const readRequestAsIpcRequest = async (request_init) => {
    let body = "";
    const method = request_init.method ?? "GET";
    /// 读取 body
    if (method === "POST" || method === "PUT") {
        let buffer;
        if (request_init.body instanceof ReadableStream) {
            const reader = request_init.body.getReader();
            const chunks = [];
            while (true) {
                const item = await reader.read();
                if (item.done) {
                    break;
                }
                chunks.push(item.value);
            }
            buffer = Buffer.concat(chunks);
        }
        else if (request_init.body instanceof Blob) {
            buffer = Buffer.from(await request_init.body.arrayBuffer());
        }
        else if (ArrayBuffer.isView(request_init.body)) {
            buffer = Buffer.from(request_init.body.buffer, request_init.body.byteOffset, request_init.body.byteLength);
        }
        else if (request_init.body instanceof ArrayBuffer) {
            buffer = Buffer.from(request_init.body);
        }
        else if (typeof request_init.body === "string") {
            body = request_init.body;
        }
        else if (request_init.body) {
            throw new Error(`unsupport body type: ${request_init.body.constructor.name}`);
        }
        if (buffer !== undefined) {
            body = buffer.toString("base64");
        }
    }
    /// 读取 headers
    let headers = Object.create(null);
    if (request_init.headers) {
        let req_headers;
        if (request_init.headers instanceof Array) {
            req_headers = new Headers(request_init.headers);
        }
        else if (request_init.headers instanceof Headers) {
            req_headers = request_init.headers;
        }
        else {
            headers = request_init.headers;
        }
        if (req_headers !== undefined) {
            req_headers.forEach((value, key) => {
                headers[key] = value;
            });
        }
    }
    return { method, body, headers };
};
exports.readRequestAsIpcRequest = readRequestAsIpcRequest;
/** 将 fetch 的参数进行标准化解析 */
const normalizeFetchArgs = (url, init) => {
    let _parsed_url;
    let _request_init = init;
    if (typeof url === "string") {
        _parsed_url = new URL(url);
    }
    else if (url instanceof Request) {
        _parsed_url = new URL(url.url);
        _request_init = url;
    }
    else if (url instanceof URL) {
        _parsed_url = url;
    }
    if (_parsed_url === undefined) {
        throw new Error(`no found url for fetch`);
    }
    const parsed_url = _parsed_url;
    const request_init = _request_init ?? {};
    return {
        parsed_url,
        request_init,
    };
};
exports.normalizeFetchArgs = normalizeFetchArgs;
