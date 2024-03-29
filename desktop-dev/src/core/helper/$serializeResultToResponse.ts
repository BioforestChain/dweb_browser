import { isBinary } from "../../helper/binaryHelper.ts";
import { Ipc, IpcRequest, IpcResponse } from "../ipc/index.ts";
import type { $Schema2, $Schema2ToType } from "./types.ts";

/**
 * @TODO 实现模式匹配
 */

export const $serializeResultToResponse = <S extends $Schema2>(schema: S) => {
  type O = $Schema2ToType<S>;
  return (request: IpcRequest, result: O, ipc: Ipc) => {
    if (result instanceof Response) {
      return IpcResponse.fromResponse(request.req_id, result, ipc);
    }
    if (isBinary(result)) {
      return IpcResponse.fromBinary(request.req_id, 200, undefined, result, ipc);
    } else if (result instanceof ReadableStream) {
      return IpcResponse.fromStream(request.req_id, 200, undefined, result, ipc);
    }
    return IpcResponse.fromJson(request.req_id, 200, undefined, result, ipc);
  };
};
