import { PromiseOut } from "../../helper/PromiseOut.ts";
import { createSignal } from "../../helper/createSignal.ts";
import { webViewMap } from "../tool/app.handle.ts";
import type { WebViewState } from "../tool/tool.event.ts";
import {
  nativeActivate,
  nativeOpen,
} from "../tool/tool.native.ts";
import { IpcHeaders, IpcResponse } from "../../core/ipc/index.ts";

const main = async () => {
  /**给前端的api服务 */

  // setTimeout(async () => {
  //   const apiServer = await http.createHttpDwebServer(jsProcess, {
  //     subdomain: "0",
  //     port: 443,
  //   });
  //   // 自己api处理 Fetch
  //   const apiReadableStreamIpc = await apiServer.listen();
  // },3000)
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "0",
    port: 443,
  });
  // 自己api处理 Fetch
  const apiReadableStreamIpc = await apiServer.listen();
  apiServer.close();
  apiReadableStreamIpc.close();
  let server = await http.createHttpDwebServer(jsProcess,{});
  let streamIpc = await server.listen();
  streamIpc.onRequest((request, ipc) => {
    console.log('接收到奥了请求 desktop_2')
    streamIpc.postMessage(
      IpcResponse.fromText(
        request.req_id,
        200,
        new IpcHeaders({
          "Content-Type": "text/html",
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Headers": "*", // 要支持 X-Dweb-Host
          "Access-Control-Allow-Methods": "*",
        }),
        `<html><div>dev_2</div><html>`,
        ipc
      )
    )
  })
  

  const main_url = server.startResult.urlInfo.buildInternalUrl("/index.html").href;
  nativeOpen(main_url)
};

main();
