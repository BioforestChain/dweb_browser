/// <reference path="../../sys/js-process/js-process.worker.d.ts"/>

import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.cjs";
import { CODE as CODE_desktop_web_mjs } from "./assets/browser.web.cjs";
import { CODE as CODE_index_html } from "./assets/index.html.cjs";

import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";

/**
 * 执行入口函数
 */
export const main = async () => {
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人KWKW
  const dwebServer = await createHttpDwebServer(jsProcess, {});
  (await dwebServer.listen()).onRequest(async (request, httpServerIpc) =>
    onRequest(request, httpServerIpc)
  );

  jsProcess.fetch(`file://statusbar.sys.dweb/`);
  await openIndexHtmlAtMWebview(origin);
};

// 执行
main().catch(console.error);

/**
 * request 事件处理器
 */
async function onRequest(request: IpcRequest, httpServerIpc: Ipc){
  console.log('接受到了请求： request.parsed_url： ', request.parsed_url)
  switch(request.parsed_url.pathname){
    case "/": 
      onRequestPathNameIndexHtml(request, httpServerIpc); 
      break;
    case "/index.html": 
      onRequestPathNameIndexHtml(request, httpServerIpc); 
      break;
    case "/browser.web.mjs": 
      onRequestPathNameBroserWebMjs(request, httpServerIpc); 
      break;
    case "/download": 
      onRequestPathNameDownload(request, httpServerIpc); 
      break;
    case "/appsinfo": 
      onRequestPathNameAppsInfo(request, httpServerIpc); 
      break;
    case `${
      request.parsed_url.pathname.startsWith("/icon") 
      ? request.parsed_url.pathname 
      : "**eot**"
    }`: 
      onRequestPathNameIcon(request, httpServerIpc); 
      break;
    case `/install`: 
      onRequestPathNameInstall(request, httpServerIpc); 
      break;
    case `/open`:
      onRequestPathNameOpen(request, httpServerIpc); 
      break;
    case "/operation_from_plugins": 
      onRequestPathOperation(request, httpServerIpc); 
      break;
    case "/open_webview": 
      onRequestPathOpenWebview(request, httpServerIpc); 
      break;
    default: 
      onRequestPathNameNoMatch(request, httpServerIpc); 
      break;
  }
}

/**
 * onRequest 事件处理器 pathname === "/" | "index.html"
 */
async function onRequestPathNameIndexHtml(
  request: IpcRequest,
  httpServerIpc: Ipc
) {
  // 拼接 html 字符串
  const url = `file://plugins.sys.dweb/get`;
  const result = `<body><script type="text/javascript">${await jsProcess
    .fetch(url)
    .text()}</script>`;
  let html = (await CODE_index_html(request)).replace("<body>", result);

  httpServerIpc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "text/html",
      }),
      html,
      httpServerIpc
    )
  );
}

/**
 * onRequest 事件处理器 pathname === "/browser.web.mjs"
 * @param request
 * @param httpServerIpc
 */
async function onRequestPathNameBroserWebMjs(
  request: IpcRequest,
  httpServerIpc: Ipc
) {
  httpServerIpc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "application/javascript",
      }),
      await CODE_desktop_web_mjs(request),
      httpServerIpc
    )
  );
}

/**
 * onRequest 事件处理器 pathname === "/download"
 * @param request
 * @param httpServerIpc
 */
async function onRequestPathNameDownload(
  request: IpcRequest,
  httpServerIpc: Ipc
) {
  const url = `file://file.sys.dweb${request.url}`;
  jsProcess
    .fetch(url)
    .then(async (res: Response) => {
      // 这里要做修改 改为 IpcResponse.fromResponse
      httpServerIpc.postMessage(
        await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
      );
    })
    .catch((err: any) => console.log("请求失败： ", err));
}

/**
 * onRequest 事件处理器 pathname === "/appsinfo"
 * @param request
 * @param httpServerIpc
 */
async function onRequestPathNameAppsInfo(
  request: IpcRequest,
  httpServerIpc: Ipc
) {
  const url = `file://file.sys.dweb/appsinfo`;
  jsProcess;
  fetch(url)
    .then(async (res: Response) => {
      // 转发给 html
      httpServerIpc.postMessage(
        await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
      );
    })
    .catch((err) => {
      console.log("获取全部的 appsInfo 失败： ", err);
    });
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
  console.log('arr:', arr, path)
  const id = arr[2];
  const iconname = arr[4];
  const url = `file://file.sys.dweb/icon?appId=${id}&name=${iconname}`;
  jsProcess;
  fetch(url)
  .then(async(res: Response) => {
    // "image/svg+xml"
    // 转发给 html
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(request.req_id,res,httpServerIpc)
    );
  })
  .catch(err => {
    console.log('获取icon 资源 失败： ', err)
  })
}

/**
 * onRequest 事件处理器 pathname === install"
 * @param request
 * @param httpServerIpc
 */
async function onRequestPathNameInstall(
  request: IpcRequest,
  httpServerIpc: Ipc
) {
  const _url = `file://app.sys.dweb${request.url}`;
  jsProcess;
  fetch(_url).then(async (res: Response) => {
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
    );
  });
}

/**
 * onRequest 事件处理器 pathname ===  open"
 * @param request
 * @param httpServerIpc
 */
async function onRequestPathNameOpen(request: IpcRequest, httpServerIpc: Ipc) {
  const _url = `file://app.sys.dweb${request.url}`;
  jsProcess;
  fetch(_url).then(async (res: Response) => {
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
    );
  });
}

/**
 * onRequest 事件处理器 pathname ===  /operation"
 * @param request
 * @param httpServerIpc
 */
async function onRequestPathOperation(request: IpcRequest, httpServerIpc: Ipc) {
  const _path = request.headers.get("plugin-target");
  const _appUrl = request.parsed_url.searchParams.get("app_url");
  const _url = `file://api.sys.dweb/${_path}?app_url=${_appUrl}`;
  jsProcess;
  fetch(_url, {
    method: request.method,
    body: request.body.raw,
    headers: request.headers,
  })
    .then(async (res: Response) => {
      console.log("[browser.worker.mts onRequestPathOperation res:]", res);
      httpServerIpc.postMessage(
        await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
      );
    })
    .then(async (err) => {
      console.log("[browser.worker.mts onRequestPathOperation err:]", err);
    });
}

/**
 * onRequest 事件处理器 pathname ===  /open_webview" 
 * @param request 
 * @param httpServerIpc 
 */
async function onRequestPathOpenWebview(request: IpcRequest, httpServerIpc: Ipc){
  const mmid = request.parsed_url.searchParams.get('mmid')
  // this.fetch(`file://dns.sys.dweb/open?app_id=${mmid}`);
  // 启动
  jsProcess
  .fetch(`file://dns.sys.dweb/open?app_id=${mmid}`)
  .then(async (res: any) => {
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(
        request.req_id,
        res,
        httpServerIpc
      )
    );
  })
  .catch((err: any) => console.log('err:', err))
}

/**
 * onRequest 事件处理器 pathname === no match" 
 * @param request 
 * @param httpServerIpc 
 */
async function onRequestPathNameNoMatch(
  request: IpcRequest,
  httpServerIpc: Ipc
) {
  httpServerIpc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      404,
      undefined,
      "No Found",
      httpServerIpc
    )
  );
}

/**
 * 打开对应的 html
 * @param origin
 * @returns
 */
async function openIndexHtmlAtMWebview(origin: string) {
  console.log("--------broser.worker.mts, origin: ", origin);
  const view_id = await jsProcess
    .fetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(origin)}`)
    .text();
  return view_id;
}
