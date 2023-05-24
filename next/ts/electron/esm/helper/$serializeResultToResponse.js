import { IpcResponse } from "../core/ipc/index.js";
import { isBinary } from "./binaryHelper.js";
/**
 * @TODO 实现模式匹配
 */
export const $serializeResultToResponse = (schema) => {
    return (request, result, ipc) => {
        if (result instanceof Response) {
            return IpcResponse.fromResponse(request.req_id, result, ipc);
        }
        if (isBinary(result)) {
            return IpcResponse.fromBinary(request.req_id, 200, undefined, result, ipc);
        }
        else if (result instanceof ReadableStream) {
            return IpcResponse.fromStream(request.req_id, 200, undefined, result, ipc);
        }
        return IpcResponse.fromJson(request.req_id, 200, undefined, result, ipc);
    };
};
