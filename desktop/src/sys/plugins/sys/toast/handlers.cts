import querystring from "querystring"
import type { Ipc, IpcRequest } from "../../../../core/ipc/index.cjs";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.cjs";
import type { ToastNMM } from "./toast.main.cjs"

/**
 * 设置状态
 * @param this 
 * @param args 
 * @param client_ipc 
 * @param ipcRequest 
 * @returns 
 */
export async function show(
  this: ToastNMM,
  args: $Schema1ToType<{}>,
  client_ipc: Ipc, 
  ipcRequest: IpcRequest
){
  const host = ipcRequest.parsed_url.host;
  const pathname = ipcRequest.parsed_url.pathname;
  const search = ipcRequest.parsed_url.search;
  const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`
  const result = await this.nativeFetch(url)
  return result;
}
