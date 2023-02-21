/// <reference path="../../sys/js-process/js-process.worker.d.ts"/>

import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { createHttpDwebServer } from "../../sys/http-server/$listenHelper.cjs";
import { CODE as CODE_desktop_web_mjs } from "./assets/browser.web.cjs";
import { CODE as CODE_index_html } from "./assets/index.html.cjs";

import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs"
import type { Ipc } from "../../core/ipc/ipc.cjs"

/**
 * 执行入口函数
 */
export const main = async () => {
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人KWKW
  const { origin, start } = await createHttpDwebServer(jsProcess, {});
  (await start()).onRequest(async (request, httpServerIpc) => onRequest(request, httpServerIpc) );
  await openIndexHtmlAtMWebview(origin)
};

// 执行
main().catch(console.error);

/**
 * request 事件处理器
 */
async function onRequest(request: IpcRequest, httpServerIpc: Ipc){
  // console.log('request.parsed_url: ', request.parsed_url)
  // console.log(' "/"+request.parsed_url.pathname.split("/")[0]:',  "/"+request.parsed_url.pathname.split("/")[0])
  // console.log('request.parsed_url.pathname.split("/": ', request.parsed_url.pathname.split("/"))
  // console.log('request.parsed_url.pathname.startsWith("/icon"): ',request.parsed_url.pathname.startsWith("/icon"))
  console.log("------------------------",`${request.parsed_url.pathname.startsWith("/icon") ? request.parsed_url.pathname : "**eot**"}`)
  switch(request.parsed_url.pathname){
    case "/": onRequestPathNameIndexHtml(request, httpServerIpc); break;
    case "/index.html": onRequestPathNameIndexHtml(request, httpServerIpc); break;
    case "/browser.web.mjs": onRequestPathNameBroserWebMjs(request, httpServerIpc); break;
    case "/download": onRequestPathNameDownload(request, httpServerIpc); break;
    case "/appsinfo": onRequestPathNameAppsInfo(request, httpServerIpc); break;
    case `${request.parsed_url.pathname.startsWith("/icon") ? request.parsed_url.pathname : "**eot**"}`: onRequestPathNameIcon(request, httpServerIpc); break;
    default: onRequestPathNameNoMatch(request, httpServerIpc); break;
  }

  
}

/**
 * onRequest 事件处理器 pathname === "/" | "index.html"
 */
async function onRequestPathNameIndexHtml(request: IpcRequest, httpServerIpc: Ipc){
  httpServerIpc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      200,
      // code_index_html 是第三方的 内容 如何增加状态栏？？
      await CODE_index_html(request),
      new IpcHeaders({
        "Content-Type": "text/html",
      })
    )
  );
}

/**
 * onRequest 事件处理器 pathname === "/browser.web.mjs" 
 * @param request 
 * @param httpServerIpc 
 */
async function onRequestPathNameBroserWebMjs(request: IpcRequest, httpServerIpc: Ipc){
  httpServerIpc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      200,
      await CODE_desktop_web_mjs(request),
      new IpcHeaders({
        "Content-Type": "application/javascript",
      })
    )
  );
}

/**
 * onRequest 事件处理器 pathname === "/download" 
 * @param request 
 * @param httpServerIpc 
 */
async function onRequestPathNameDownload(request: IpcRequest, httpServerIpc: Ipc){
  const url = `file://file.sys.dweb${request.url}`
  jsProcess
  .fetch(url)
  .then(async (res: Response) => {
    // 这里要做修改 改为 IpcResponse.fromResponse
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(
        request.req_id,
        res,
        httpServerIpc
      )
    );
   
  })
  .catch((err: any) => console.log('请求失败： ', err))
}

/**
 * onRequest 事件处理器 pathname === "/appsinfo" 
 * @param request 
 * @param httpServerIpc 
 */
async function onRequestPathNameAppsInfo(request: IpcRequest, httpServerIpc: Ipc){
  const url = `file://file.sys.dweb/appsinfo`
  
  jsProcess
  fetch(url)
  .then(async(res: Response) => {
    // 转发给 html
    console.log("转发给 html 成功")
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(
        request.req_id,
        res,
        httpServerIpc
      )
    );
  })
  .catch(err => {
    console.log('获取全部的 appsInfo 失败： ', err)
  })
}

/**
 * onRequest 事件处理器 pathname === icon
 * @param request 
 * @param httpServerIpc 
 */
async function onRequestPathNameIcon(request: IpcRequest, httpServerIpc: Ipc){
  console.log("获取icon")
  const path = request.parsed_url.pathname
  const arr = path.split("/")
  console.log('arr:', arr)
  const id = arr[2];
  const iconname= arr[4];
  const url =  `file://file.sys.dweb/icon?appId=${id}&name=${iconname}`
  jsProcess
  fetch(url)
  .then(async(res: Response) => {
    // "image/svg+xml"
    // 转发给 html
    console.log("转发图片资源: ", res)
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(
        request.req_id,
        res,
        httpServerIpc
      )
    );
  })
  .catch(err => {
    console.log('获取icon 资源 失败： ', err)
  })
}

/**
 * onRequest 事件处理器 pathname === no match" 
 * @param request 
 * @param httpServerIpc 
 */
async function onRequestPathNameNoMatch(request: IpcRequest, httpServerIpc: Ipc){
  httpServerIpc.postMessage(
    IpcResponse.fromText(request.req_id, 404, "No Found")
  );
}



/**
 * 打开对应的 html
 * @param origin 
 * @returns 
 */
async function openIndexHtmlAtMWebview(origin:string){
  const view_id = await jsProcess
                        .fetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(origin)}`)
                        .text();
  return view_id
}