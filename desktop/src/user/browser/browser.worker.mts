/// <reference path="../../sys/js-process/js-process.worker.d.ts"/>

// import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
// import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
// import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.cjs";
// import html from "../../../assets/html/browser.html"

// import type { Ipc } from "../../core/ipc/ipc.cjs";
// import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
// import { streamReadAll } from "../../helper/readableStreamHelper.cjs";
import { wwwServerOnRequest } from "./www-server-on-request.mjs"
import { apiServerOnRequest } from "./api-server-on-request.mjs"
import chalk from "chalk"

import type { $OnIpcEventMessage } from "../../core/ipc/const.cjs"

 

 

const main = async () => {
  console.log('[browser.worker.mts]')
  const wwwServer = await http.createHttpDwebServer(jsProcess,{subdomain: "www", port: 443});
  const apiServer = await http.createHttpDwebServer(jsProcess,{subdomain: "api", port: 443});

  ;(await wwwServer.listen()).onRequest(wwwServerOnRequest)
  ;(await apiServer.listen()).onRequest(apiServerOnRequest)

  {
    const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href
    console.log("cot#open interUrl=>", interUrl)
    const view_id = await jsProcess
      .nativeFetch(
        `file://mwebview.sys.dweb/open?url=${encodeURIComponent(interUrl)}`
      )
      .text();
  }

   
  // 等待他人的连接
  {
    jsProcess.onConnect((ipc) => {
      console.log('browser.worker.mts onConnect')
      ipc.onEvent((event, ipc) => {
        console.log("got event:", ipc.remote.mmid, event.name, event.text);
        setTimeout(() => {
          ipc.postMessage(IpcEvent.fromText(event.name, "echo:" + event.text));
        }, 500);
      });
    });
  }

  // 后期需要更改 把这个移动到 js-process.worker.mts 中
  // 只接连接全部的plugins
  const { IpcEvent } = ipc;
  const statusbarSysDwebIpc = await createConnect("statusbar.sys.dweb");
  statusbarSysDwebIpc.onEvent((event, ipc) => {
    console.error("got event:", ipc.remote.mmid, event.name, event.text);
  });
  statusbarSysDwebIpc.postMessage({value: "value"});

  async function createConnect(mmid: $MMID){
    const ipc = await jsProcess.connect(mmid);
    return {
      // onMessage:ipc.onMessage,
      // 暂时只暴露onEvent
      onEvent: (cb: $OnIpcEventMessage) => ipc.onEvent(cb),
      // onReqeust: ipc.onRequest,
      postMessage: async (data: unknown /** JSON 字符串 */) => {
        let str = ""
        try{
          str = JSON.stringify(data)
        }catch(err){
          throw new Error(`非法的 data 无法转化为 json`)
        }
        ipc.postMessage(IpcEvent.fromText(mmid, str))
      },
      ipc: ipc
    }
  }
}

main();



 



















// /**
//  * 执行入口函数
//  */
// export const main = async () => {
//   /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人KWKW
//   // 如何启动这个？？ 现在是个问题
//   const dwebServer = await createHttpDwebServer(jsProcess, {});
//   (await dwebServer.listen()).onRequest(async (request, httpServerIpc) =>
//     onRequest(request, httpServerIpc)
//   );

//   await openIndexHtmlAtMWebview(
//     dwebServer.startResult.urlInfo.buildInternalUrl((url) => {
//       url.pathname = "/index.html";
//     }).href
//   );
// };

// // 执行
// main().catch(console.error);

// /**
//  * request 事件处理器
//  */
// async function onRequest(request: IpcRequest, httpServerIpc: Ipc) {
//   console.log("接受到了请求： request.parsed_url.pathname： ", request.parsed_url.pathname);
//   switch (request.parsed_url.pathname) {
//     case "/":
//     case "/index.html":
//       onRequestPathNameIndexHtml(request, httpServerIpc);
//       break;
//     case "/download": 
//       onRequestPathNameDownload(request, httpServerIpc); 
//       break;
//     case "/appsinfo": 
//       onRequestPathNameAppsInfo(request, httpServerIpc); 
//       break;
//     case `${
//       request.parsed_url.pathname.startsWith("/icon") 
//       ? request.parsed_url.pathname 
//       : "**eot**"
//     }`: 
//       onRequestPathNameIcon(request, httpServerIpc); 
//       break;
//     case `/install`: 
//       onRequestPathNameInstall(request, httpServerIpc); 
//       break;
//     case `/open`:
//       onRequestPathNameOpen(request, httpServerIpc); 
//       break;
//     case "/operation_from_plugins": 
//       onRequestPathOperation(request, httpServerIpc); 
//       break;
//     case "/open_webview": 
//       onRequestPathOpenWebview(request, httpServerIpc); 
//       break;
//     default: 
//       onRequestPathNameNoMatch(request, httpServerIpc); 
//       break;
//   }
// }

// /**
//  * onRequest 事件处理器 pathname === "/" | "index.html"
//  */
// async function onRequestPathNameIndexHtml(
//   request: IpcRequest,
//   httpServerIpc: Ipc
// ) {
//   // 拼接 html 字符串
//   const url = `file://plugins.sys.dweb/get`;
//   const result = `<body><script type="text/javascript">${await jsProcess
//     .nativeFetch(url)
//     .text()}</script>`;
//     // let html = (await CODE_index_html(request)).replace("<body>", result);
//   let _html = html.replace("<body>", result)
//   httpServerIpc.postMessage(
//     IpcResponse.fromText(
//       request.req_id,
//       200,
//       new IpcHeaders({
//         "Content-Type": "text/html",
//       }),
//       _html,
//       httpServerIpc
//     )
//   );
// }

// /**
//  * onRequest 事件处理器 pathname === "/download"
//  * @param request
//  * @param httpServerIpc
//  */
// async function onRequestPathNameDownload(
//   request: IpcRequest,
//   httpServerIpc: Ipc
// ) {
//   const url = `file://file.sys.dweb${request.url}`;
//   jsProcess
//     .nativeFetch(url)
//     .then(async (res: Response) => {
//       // 这里要做修改 改为 IpcResponse.fromResponse
//       httpServerIpc.postMessage(
//         await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
//       );
//     })
//     .catch((err: any) => console.log("请求失败： ", err));
// }

// /**
//  * onRequest 事件处理器 pathname === "/appsinfo"
//  * @param request
//  * @param httpServerIpc
//  */
// async function onRequestPathNameAppsInfo(
//   request: IpcRequest,
//   httpServerIpc: Ipc
// ) {

//   const url = `file://file.sys.dweb/appsinfo`;

//   jsProcess;
//   fetch(url)
//   .then(async (res: Response) => {
//     // 可以正常的读取完整的数据
//     // console.log('res: ', res)
//     // const reader = res.body?.getReader()
//     // let loop = false
//     // let arr: Uint8Array | undefined;
//     // do{
//     //     const {value, done} = await reader?.read() as any;
//     //     loop = !done;
//     //     console.log('done', done)
//     //     console.log('value: ', value)
//     //     if(value){
//     //       if(arr){
//     //         arr = Uint8Array.from([...arr, ...value])
//     //       }else{
//     //         arr = Uint8Array.from([...value])
//     //       }
//     //     }
//     //     // console.log(new TextDecoder().decode(value.buffer))
//     // }while(loop)
//     // console.log('读取完毕', new TextDecoder().decode(arr))
//     // 转发给 html

//     const ipcResponse = await IpcResponse.fromResponse(
//       request.req_id,
//       res,
//       httpServerIpc
//     )
//     console.log("stream id:",ipcResponse.body.metaBody.streamId)
    

//     // fromResponse 无法读取完成的数据
//     httpServerIpc.postMessage(
//       // await IpcResponse.fromJson(
//       //   request.req_id, 
//       //   200,
//       //   new IpcHeaders({
//       //     "content-type": "application/json"
//       //   }),
//       //   await res.json(), 
//       //   httpServerIpc
//       // )
//       ipcResponse
//     );
//     console.log('browser.worker.mts 接受到了 appsifo2: ')
//   })
//   .catch((err) => {
//     console.log("获取全部的 appsInfo 失败： ", err);
//   });

// }

// /**
//  * onRequest 事件处理器 pathname === icon
//  * @param request
//  * @param httpServerIpc
//  */
// async function onRequestPathNameIcon(request: IpcRequest, httpServerIpc: Ipc){
//   console.log("获取icon")
//   const path = request.parsed_url.pathname
//   const arr = path.split("/")
//   console.log('arr:', arr, path)
//   const id = arr[2];
//   const iconname = arr[4];
//   const url = `file://file.sys.dweb/icon?appId=${id}&name=${iconname}`;
//   jsProcess;
//   fetch(url)
//   .then(async(res: Response) => {
//     // "image/svg+xml"
//     // 转发给 html
//     httpServerIpc.postMessage(
//       await IpcResponse.fromResponse(request.req_id,res,httpServerIpc)
//     );
//   })
//   .catch(err => {
//     console.log('获取icon 资源 失败： ', err)
//   })
// }

// /**
//  * onRequest 事件处理器 pathname === install"
//  * @param request
//  * @param httpServerIpc
//  */
// async function onRequestPathNameInstall(
//   request: IpcRequest,
//   httpServerIpc: Ipc
// ) {
//   const _url = `file://jmm.sys.dweb${request.url}`;
//   jsProcess.nativeFetch(_url).then(async (res: Response) => {
//     httpServerIpc.postMessage(
//       await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
//     );
//   });
// }

// /**
//  * onRequest 事件处理器 pathname ===  open"
//  * @param request
//  * @param httpServerIpc
//  */
// async function onRequestPathNameOpen(request: IpcRequest, httpServerIpc: Ipc) {
//   const _url = `file://jmm.sys.dweb${request.url}`;
//   jsProcess.nativeFetch(_url).then(async (res: Response) => {
//     httpServerIpc.postMessage(
//       await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
//     );
//   });
// }

// /**
//  * onRequest 事件处理器 pathname ===  /operation"
//  * @param request
//  * @param httpServerIpc
//  */
// async function onRequestPathOperation(request: IpcRequest, httpServerIpc: Ipc) {
//   const _path = request.headers.get("plugin-target");
//   const _appUrl = request.parsed_url.searchParams.get("app_url");
//   const _url = `file://api.sys.dweb/${_path}?app_url=${_appUrl}`;
//   jsProcess
//     .nativeFetch(_url, {
//       method: request.method,
//       body: request.body.raw,
//       headers: request.headers,
//     })
//     .then(async (res: Response) => {
//       console.log("[browser.worker.mts onRequestPathOperation res:]", res);
//       httpServerIpc.postMessage(
//         await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
//       );
//     })
//     .then(async (err) => {
//       console.log("[browser.worker.mts onRequestPathOperation err:]", err);
//     });
// }

// /**
//  * onRequest 事件处理器 pathname ===  /open_webview" 
//  * @param request 
//  * @param httpServerIpc 
//  */
// async function onRequestPathOpenWebview(request: IpcRequest, httpServerIpc: Ipc){
//   const mmid = request.parsed_url.searchParams.get('mmid')
//   // 启动
//   jsProcess
//   .nativeFetch(`file://dns.sys.dweb/open?app_id=${mmid}`)
//   .then(async (res: any) => {
//     console.log('返回跳转到下载页面')
//     const json = await res.json()
//     httpServerIpc.postMessage(
//       IpcResponse.fromJson(
//         request.req_id,
//         res.status,
//         new IpcHeaders({
//           'content-type': "appliction/json; chrset=UTF-8"
//         }),
//         json,
//         httpServerIpc
//       )
//     );
//   })
//   .catch((err: any) => console.log('err:', err))
// }

// /**
//  * onRequest 事件处理器 pathname === no match" 
//  * @param request 
//  * @param httpServerIpc 
//  */
// async function onRequestPathNameNoMatch(
//   request: IpcRequest,
//   httpServerIpc: Ipc
// ) {
//   httpServerIpc.postMessage(
//     IpcResponse.fromText(
//       request.req_id,
//       404,
//       undefined,
//       "No Found",
//       httpServerIpc
//     )
//   );
// }

// /**
//  * 打开对应的 html
//  * @param url
//  * @returns
//  */
// async function openIndexHtmlAtMWebview(url: string) {
//   console.log("--------broser.worker.mts, url: ", url);
//   const view_id = await jsProcess
//     .nativeFetch(
//       `file://mwebview.sys.dweb/open?url=${encodeURIComponent(url)}`
//     )
//     .text();
//   return view_id;
// }
