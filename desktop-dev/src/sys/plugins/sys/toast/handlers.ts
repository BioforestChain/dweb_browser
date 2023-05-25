import type { Ipc, IpcRequest } from "../../../../core/ipc/index.ts";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.ts";
import type { ToastNMM } from "./toast.main.ts";

/**
 * 设置状态
 * @param this
 * @param _args
 * @param _client_ipc
 * @param ipcRequest
 * @returns
 */
export async function show(
  this: ToastNMM,
  _args: $Schema1ToType<$Schema1>,
  _client_ipc: Ipc,
  ipcRequest: IpcRequest
) {
  const host = ipcRequest.parsed_url.host;
  const pathname = ipcRequest.parsed_url.pathname;
  const search = ipcRequest.parsed_url.search;
  const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`;
  const result = await this.nativeFetch(url);
  return result;
}
