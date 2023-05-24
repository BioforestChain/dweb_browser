import { binaryToU8a, isBinary } from "./binaryHelper.js";
import { simpleEncoder } from "./encoding.js";
import { headersToRecord } from "./headersToRecord.js";
import { httpMethodCanOwnBody } from "./httpMethodCanOwnBody.js";
/** 将 request 参数解构 成 ipcRequest 的参数 */
export const $readRequestAsIpcRequest = async (request_init) => {
    let body = "";
    const method = request_init.method ?? "GET";
    /// 读取 body
    if (httpMethodCanOwnBody(method)) {
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
        else if (isBinary(request_init.body)) {
            body = binaryToU8a(request_init.body);
        }
        else if (typeof request_init.body === "string") {
            body = simpleEncoder(request_init.body, "utf8");
        }
        else if (request_init.body) {
            throw new Error(`unsupport body type: ${request_init.body.constructor.name}`);
        }
    }
    /// 读取 headers
    const headers = headersToRecord(request_init.headers);
    return { method, body, headers };
};
