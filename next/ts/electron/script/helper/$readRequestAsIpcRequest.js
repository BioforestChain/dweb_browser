"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.$readRequestAsIpcRequest = void 0;
const binaryHelper_js_1 = require("./binaryHelper.js");
const encoding_js_1 = require("./encoding.js");
const headersToRecord_js_1 = require("./headersToRecord.js");
const httpMethodCanOwnBody_js_1 = require("./httpMethodCanOwnBody.js");
/** 将 request 参数解构 成 ipcRequest 的参数 */
const $readRequestAsIpcRequest = async (request_init) => {
    let body = "";
    const method = request_init.method ?? "GET";
    /// 读取 body
    if ((0, httpMethodCanOwnBody_js_1.httpMethodCanOwnBody)(method)) {
        if (request_init.body instanceof ReadableStream) {
            body = request_init.body;
            // const reader = (
            //   request_init.body as ReadableStream<Uint8Array>
            // ).getReader();
            // const chunks: Uint8Array[] = [];
            // while (true) {
            //   const item = await reader.read();
            //   if (item.done) {
            //     break;
            //   }
            //   chunks.push(item.value);
            // }
            // buffer = Buffer.concat(chunks);
        }
        else if (request_init.body instanceof Blob) {
            body = new Uint8Array(await request_init.body.arrayBuffer());
        }
        else if ((0, binaryHelper_js_1.isBinary)(request_init.body)) {
            body = (0, binaryHelper_js_1.binaryToU8a)(request_init.body);
        }
        else if (typeof request_init.body === "string") {
            body = (0, encoding_js_1.simpleEncoder)(request_init.body, "utf8");
        }
        else if (request_init.body) {
            throw new Error(`unsupport body type: ${request_init.body.constructor.name}`);
        }
    }
    /// 读取 headers
    const headers = (0, headersToRecord_js_1.headersToRecord)(request_init.headers);
    return { method, body, headers };
};
exports.$readRequestAsIpcRequest = $readRequestAsIpcRequest;
