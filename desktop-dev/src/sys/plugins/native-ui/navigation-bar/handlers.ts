import type { Ipc, IpcRequest } from "../../../../core/ipc/index.ts";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.ts";
import type { NavigationBarNMM } from "./navigation-bar.main.ts";

/**
 * 开启监听
 * @param this
 * @param _args
 * @param clientIpc
 * @param _request
 * @returns
 */
export function startObserve(
  this: NavigationBarNMM,
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
 * @param clientIpc
 * @param request
 * @returns
 */
export function stopObserve(
  this: NavigationBarNMM,
  _args: $Schema1ToType<$Schema1>,
  clientIpc: Ipc,
  _request: IpcRequest
) {
  this.observesState.set(clientIpc.remote.mmid, false);
  return true;
}
