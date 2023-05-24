import querystring from "querystring"
import type { Ipc, IpcRequest } from "../../../../core/ipc/index.js";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.js";
import type { TorchNMM } from "./torch.main.js"

 
export async function toggleTorch(
  this: TorchNMM,
  args: $Schema1ToType<$Schema1>,
  client_ipc: Ipc, 
  ipcRequest: IpcRequest
){
  // const search = querystring.unescape(ipcRequest.url).split("?")[1]
  const host = ipcRequest.parsed_url.host;
  const pathname = ipcRequest.parsed_url.pathname;
  const search = ipcRequest.parsed_url.search;
  const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`
  const result = await this.nativeFetch(url)
  return result;
}

 
export async function torchState(
  this: TorchNMM,
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
