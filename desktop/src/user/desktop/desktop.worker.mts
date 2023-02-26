/// <reference path="../../sys/js-process/js-process.worker.d.ts"/>

import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.cjs";
import { CODE as CODE_desktop_web_mjs } from "./assets/desktop.web.mjs.cjs";
import { CODE as CODE_index_html } from "./assets/index.html.cjs";

console.log("ookkkkk, i'm in worker");

export const main = async () => {
  debugger;
  if (jsProcess.meta.optionalBoolean("debug")) {
    await new Promise((resolve) => {
      Object.assign(self, { start_main: resolve });
    });
  }
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
  const httpDwebServer = await createHttpDwebServer(jsProcess, {});
  (await httpDwebServer.listen()).onRequest(async (request, httpServerIpc) => {
    if (
      request.parsed_url.pathname === "/" ||
      request.parsed_url.pathname === "/index.html"
    ) {
      /// 收到请求
      httpServerIpc.postMessage(
        IpcResponse.fromText(
          request.req_id,
          200,
          new IpcHeaders({
            "Content-Type": "text/html",
          }),
          await CODE_index_html(request),
          httpServerIpc
        )
      );
    } else if (request.parsed_url.pathname === "/desktop.web.mjs") {
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
    } else {
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
  });

  console.log("http 服务创建成功");

  const main_url =
    httpDwebServer.startResult.urlInfo.buildInternalUrl("/index.html").href;
  console.log("打开浏览器页面", main_url);
  {
    const view_id = await jsProcess
      .fetch(
        `file://mwebview.sys.dweb/open?url=${encodeURIComponent(main_url)}`
      )
      .text();
  }
};
main().catch(console.error);
