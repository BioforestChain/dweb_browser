import { Ipc, IpcRequest, IpcResponse } from "../../core/ipc/index.ts";
import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import { getAllApps } from "../jmm/jmm.api.serve.ts"
 
import type { BrowserNMM } from "./browser.ts";

export async function createAPIServer(this: BrowserNMM) {
  // 为 下载页面做 准备
  this.apiServer = await createHttpDwebServer(this, {
    subdomain: "api",
    port: 433,
  });
  const apiReadableStreamIpc = await this.apiServer.listen();
  apiReadableStreamIpc.onRequest(onRequest.bind(this));
}

async function onRequest(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  const pathname = request.parsed_url.pathname;
  switch(pathname){
    case "/appsinfo":
      getAppsInfo.bind(this)(request, ipc)
      break;
    default: console.error("browser", "还有没有匹配的 api 请求 pathname ===", pathname)
  }
}

async function getAppsInfo(
  this: BrowserNMM, 
  request: IpcRequest, 
  ipc: Ipc
){
  const appsInfo = await getAllApps()
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id, 
      200, 
      undefined,
      JSON.stringify(appsInfo),
      ipc, 
    )
  )
}