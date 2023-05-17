import type { Ipc, IpcRequest } from "../../core/ipc/index.cjs";
import type { $Schema1, $Schema1ToType } from "../../helper/types.cjs";
import type { JmmNMM } from "./jmm.cjs"

/**
 * 功能：
 * 打开一个新的 webveiw 页面
 * @param this 
 * @param args 
 * @param client_ipc 
 * @param ipcRequest 
 * @returns 
 */
export async function install(
  this: JmmNMM,
  args: $Schema1ToType<{metadataUrl: "string" }>,
  client_ipc: Ipc, 
  ipcRequest: IpcRequest
){
  // 需要同时查询参数传递进去
  if(this.wwwServer === undefined) throw new Error(`this.wwwServer === undefined`)
  const interUrl = this.wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
    url.pathname = "/index.html";
  }).href;
  const url = `file://mwebview.sys.dweb/open_new_webveiw_at_focused?url=${interUrl}`
  const body = JSON.stringify({metadataUrl: args.metadataUrl})
  await this.nativeFetch(url,{
    method: "POST",
    body
  })
  return true;
}