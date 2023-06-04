import { Ipc, IpcRequest, IpcResponse, IpcHeaders } from "../../core/ipc/index.ts";
import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import { getAllApps } from "../jmm/jmm.api.serve.ts"
import type { BrowserNMM } from "./browser.ts";

export async function createAPIServer(this: BrowserNMM) {
  // 为 下载页面做 准备
  this.apiServer = await createHttpDwebServer(this, {
    subdomain: "api",
    port: 433,
  });
  // 数据体
  // ServerUrlInfo {
  //   host: 'api.browser.dweb:433',
  //   internal_origin: 'http://api.browser.dweb-433.localhost:22605',
  //   public_origin: 'http://localhost:22605'
  // }
  const apiReadableStreamIpc = await this.apiServer.listen();
  apiReadableStreamIpc.onRequest(onRequest.bind(this));
}

async function onRequest(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  const pathname = request.parsed_url.pathname;
  switch(pathname){
    case "/appsinfo":
      getAppsInfo.bind(this)(request, ipc);
      break;
    case "/update/content":
      updateContent.bind(this)(request, ipc);
      break;
    case "/can-go-back":
      canGoBack.bind(this)(request, ipc);
      break;
    case "/can-go-forward":
      canGoForward.bind(this)(request, ipc);
      break;
    case "/go-back":
      goBack.bind(this)(request, ipc);
      break;
    case "/go-forward":
      goForward.bind(this)(request, ipc);
      break;
    case "/refresh":
      refresh.bind(this)(request, ipc);
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

async function updateContent(
  this: BrowserNMM, 
  request: IpcRequest, 
  ipc: Ipc
){
  const href = request.parsed_url.searchParams.get("url");
  if(href === null){
    ipc.postMessage(
      await IpcResponse.fromText(
        request.req_id, 
        400, 
        undefined,
        "缺少 url 参数",
        ipc, 
      )
    )
    return;
  }
  const regexp = /^(https?|http):\/\/([a-z0-9-]+(.[a-z0-9-]+)*(:[0-9]+)?)(\/.*)?$/i;
  if(regexp.test(href)){
    this.contentBV.loadWithHistory(href)
    ipc.postMessage(
      await IpcResponse.fromText(
        request.req_id, 
        200, 
        undefined,
        "ok",
        ipc, 
      )
    )
    return;
  }
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id, 
      400, 
      undefined,
      "非法的 url 参数:" + href,
      ipc, 
    )
  )
}


async function canGoBack(
  this: BrowserNMM, 
  request: IpcRequest, 
  ipc: Ipc
){
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id, 
      200, 
      new IpcHeaders({
        "Content-Type": "application/json"
      }),
      JSON.stringify({
        value: this.contentBV.canGoBack()
      }),
      ipc, 
    )
  )
}

async function canGoForward(
  this: BrowserNMM, 
  request: IpcRequest, 
  ipc: Ipc
){
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id, 
      200, 
      new IpcHeaders({
        "Content-Type": "application/json"
      }),
      JSON.stringify({
        value: this.contentBV.canGoForward()
      }),
      ipc, 
    )
  )
}

async function goBack(
  this: BrowserNMM, 
  request: IpcRequest, 
  ipc: Ipc
){
  this.contentBV.goBack()
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id, 
      200, 
      new IpcHeaders({
        "Content-Type": "application/json"
      }),
      JSON.stringify({
        value: "ok"
      }),
      ipc, 
    )
  )
}

async function goForward(
  this: BrowserNMM, 
  request: IpcRequest, 
  ipc: Ipc
){
  
  this.contentBV.goForward()
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id, 
      200, 
      new IpcHeaders({
        "Content-Type": "application/json"
      }),
      JSON.stringify({
        value: "ok"
      }),
      ipc, 
    )
  )
}

async function refresh(
  this: BrowserNMM, 
  request: IpcRequest, 
  ipc: Ipc
){
  try{
    this.contentBV.reload()
  }catch(err){
    console.error('error', err)
    throw new Error(`refresh err`)
  }
  
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id, 
      200, 
      new IpcHeaders({
        "Content-Type": "application/json"
      }),
      JSON.stringify({
        value: "ok"
      }),
      ipc, 
    )
  )
}