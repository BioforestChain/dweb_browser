import type { Ipc, IpcRequest } from "../../../core/ipc/index.ts";
import type { $Schema1, $Schema1ToType } from "../../../helper/types.ts";
import type { SafeAreaNMM } from "./safe-area.main.ts";

/**
 * 开启监听
 * @param this
 * @param _args
 * @param _clientIpc
 * @param request
 * @returns
 */
export function startObserve(
  this: SafeAreaNMM,
  _args: $Schema1ToType<$Schema1>,
  clientIpc: Ipc,
  _request: IpcRequest
) {
  this.observesState.set(clientIpc.remote.mmid, true);
  return true;
}

/**
 * 停止 监听
 * @param this
 * @param _args
 * @param _clientIpc
 * @param request
 * @returns
 */
export function stopObserve(
  this: SafeAreaNMM,
  _args: $Schema1ToType<$Schema1>,
  clientIpc: Ipc,
  _request: IpcRequest
) {
  this.observesState.set(clientIpc.remote.mmid, false);
  return true;
}
