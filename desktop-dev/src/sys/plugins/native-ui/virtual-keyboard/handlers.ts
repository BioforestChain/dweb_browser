import type { Ipc, IpcRequest } from "../../../../core/ipc/index.ts";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.ts";
import type { VirtualKeyboardNMM } from "./virtual-keyboard.main.ts"

/**
 * 获取状态
 * @param this 
 * @param _args 
 * @param _client_ipc 
 * @param ipcRequest 
 * @returns 
 */
export async function getState(
  this: VirtualKeyboardNMM,
  _args: $Schema1ToType<$Schema1>,
  _client_ipc: Ipc, 
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

/**
 * 设置状态
 * @param this 
 * @param _args 
 * @param _client_ipc 
 * @param ipcRequest 
 * @returns 
 */
export async function setState(
  this: VirtualKeyboardNMM,
  _args: $Schema1ToType<$Schema1>,
  _client_ipc: Ipc, 
  ipcRequest: IpcRequest
){
  const host = ipcRequest.parsed_url.host;
  const pathname = ipcRequest.parsed_url.pathname;
  const search = ipcRequest.parsed_url.search;
  const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`
  const result = await this.nativeFetch(url)
  return result;
}

/**
 * 开启监听
 * @param this 
 * @param _args 
 * @param _clientIpc 
 * @param request 
 * @returns 
 */
export function startObserve(
  this: VirtualKeyboardNMM, 
  _args: $Schema1ToType<$Schema1>,
  clientIpc: Ipc,
  _request: IpcRequest,
){
  this.observesState.set(clientIpc.remote.mmid, true)
  return true;
}

/**
 * 停止 监听
 * @param this 
 * @param _args 
 * @param _clientIpc 
 * @param _request 
 * @returns 
 */
export function stopObserve(
  this: VirtualKeyboardNMM, 
  _args: $Schema1ToType<$Schema1>,
  clientIpc: Ipc,
  _request: IpcRequest,
){
  this.observesState.set(clientIpc.remote.mmid, false)
  return true;
}

 