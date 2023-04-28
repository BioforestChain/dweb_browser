// // 全部 dweb_app 提供 基础worker
// import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
// import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.cjs";

// import type { Ipc } from "../../core/ipc/ipc.cjs";
// import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";

// /**
//  * 执行入口函数
//  */
// export const main = async () => {
//   /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人KWKW
//   // origin === http://app.w85defe5.dweb-80.localhost:22605
//   const dwebServer = await createHttpDwebServer(jsProcess, {});
//   (await dwebServer.listen()).onRequest(async (request, httpServerIpc) => {
//     switch (request.url) {
//       case request.url.startsWith("/operation_from_plugins?")
//         ? request.url
//         : "**eot**":
//         onRequestAtOperationFromPlugins(request, httpServerIpc);
//         break;
//       default:
//         onRequestDefault(origin, request, httpServerIpc);
//         break;
//     }
//   });

//   await openIndexHtmlAtMWebview(origin);
// };

// main().catch(console.error);

// /**
//  * 打开对应的 html
//  * @param origin
//  * @returns
//  */
// async function openIndexHtmlAtMWebview(origin: string) {
//   const view_id = await jsProcess
//     .nativeFetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(origin)}`)
//     .text();
//   return view_id;
// }

// async function onRequestAtOperationFromPlugins(request: IpcRequest, ipc: Ipc) {
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
//       ipc.postMessage(await IpcResponse.fromResponse(request.req_id, res, ipc));
//     });
// }

// async function onRequestDefault(origin: string, request: IpcRequest, ipc: Ipc) {
//   const _url = `file://www.sys.dweb/server?url=${origin}${request.url}`;
//   const response = await jsProcess.nativeFetch(_url);
//   ipc.postMessage(
//     await IpcResponse.fromResponse(request.req_id, response, ipc)
//   );
// }
