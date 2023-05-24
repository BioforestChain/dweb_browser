"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.$serializeResultToResponse = void 0;
const index_js_1 = require("../core/ipc/index.js");
const binaryHelper_js_1 = require("./binaryHelper.js");
/**
 * @TODO 实现模式匹配
 */
const $serializeResultToResponse = (schema) => {
    return (request, result, ipc) => {
        if (result instanceof Response) {
            return index_js_1.IpcResponse.fromResponse(request.req_id, result, ipc);
        }
        if ((0, binaryHelper_js_1.isBinary)(result)) {
            return index_js_1.IpcResponse.fromBinary(request.req_id, 200, undefined, result, ipc);
        }
        else if (result instanceof ReadableStream) {
            return index_js_1.IpcResponse.fromStream(request.req_id, 200, undefined, result, ipc);
        }
        return index_js_1.IpcResponse.fromJson(request.req_id, 200, undefined, result, ipc);
    };
};
exports.$serializeResultToResponse = $serializeResultToResponse;
