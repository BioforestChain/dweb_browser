import { Ipc, IpcRequest, IpcResponse } from "../core/ipc/index.cjs";
import type { $Schema2, $Schema2ToType } from "./types.cjs";

/**
 * @TODO 实现模式匹配
 */

export const $serializeResultToResponse = <S extends $Schema2>(schema: S) => {
  type O = $Schema2ToType<S>;
  return (request: IpcRequest, result: O, ipc: Ipc) => {
    if (result instanceof Response) {
      return IpcResponse.fromResponse(request.req_id, result, ipc);
    }
    if (ArrayBuffer.isView(result) || result instanceof ArrayBuffer) {
      return IpcResponse.fromBinary(request.req_id, 200, result);
    }
    return IpcResponse.fromJson(request.req_id, 200, result);
  };
};
