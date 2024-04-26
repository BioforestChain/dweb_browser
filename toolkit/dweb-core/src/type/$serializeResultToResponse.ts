import { isBinary } from "@dweb-browser/helper/fun/binaryHelper.ts";
import { Ipc, IpcClientRequest, IpcResponse } from "../../src/ipc/index.ts";
import type { $Schema2, $Schema2ToType } from "./types.ts";

/**
 * @TODO 实现模式匹配
 */

export const $serializeResultToResponse = <S extends $Schema2>(schema: S) => {
  type O = $Schema2ToType<S>;
  return (request: IpcClientRequest, result: O, ipc: Ipc) => {
    if (result instanceof Response) {
      return IpcResponse.fromResponse(request.reqId, result, ipc);
    }
    if (isBinary(result)) {
      return IpcResponse.fromBinary(request.reqId, 200, undefined, result, ipc);
    } else if (result instanceof ReadableStream) {
      return IpcResponse.fromStream(request.reqId, 200, undefined, result, ipc);
    }
    return IpcResponse.fromJson(request.reqId, 200, undefined, result, ipc);
  };
};
