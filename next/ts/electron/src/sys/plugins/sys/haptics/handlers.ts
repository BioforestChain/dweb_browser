import querystring from "querystring"
import type { Ipc, IpcRequest } from "../../../../core/ipc/index.js";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.js";
import type { HapticsNMM } from "./haptics.main.js"
 
export async function setHaptics(
  this: HapticsNMM,
  args: $Schema1ToType<$Schema1>,
  client_ipc: Ipc, 
  ipcRequest: IpcRequest
){
  // const search = querystring.unescape(ipcRequest.url).split("?")[1]
  const host = ipcRequest.parsed_url.host;
  const pathname = ipcRequest.parsed_url.pathname;
  const search = ipcRequest.parsed_url.search;
  const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}&action=${pathname.slice(1)}`
  const result = await this.nativeFetch(url)
  return true;
}
