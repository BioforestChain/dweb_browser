/// <reference path="../../sys/js-process.worker.d.ts"/>

import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.cjs";
import { IPC_DATA_TYPE, IPC_ROLE } from "../../core/ipc/const.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import type { $ReqMatcher } from "../../helper/$ReqMatcher.cjs";
import { script } from "./desktop.web.cjs";

console.log("ookkkkk, i'm in worker");

export const main = async () => {
  debugger;
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
  const { origin } = await process
    .fetch(`file://localhost.sys.dweb/listen?port=80`)
    .object<{
      origin: string;
    }>();
  console.log("开始监听服务：", origin);
  (async () => {
    const html = String.raw;
    /// 创建一个基于 二进制流的 ipc 信道
    const httpServerIpc = new ReadableStreamIpc(
      new JsProcessMicroModule("localhost.sys.dweb"),
      IPC_ROLE.CLIENT,
      true
    );
    const httpIncomeRequestStream = await process
      .fetch(
        `file://localhost.sys.dweb/request/on?port=80&paths=${encodeURIComponent(
          JSON.stringify([
            {
              pathname: "/",
              matchMode: "prefix",
              method: "GET",
            },
            {
              pathname: "/",
              matchMode: "prefix",
              method: "POST",
            },
          ] satisfies $ReqMatcher[])
        )}`,
        {
          method: "POST",
          /// 这是上行的通道
          body: httpServerIpc.stream,
        }
      )
      .stream();
    console.log("开始响应服务请求");

    httpServerIpc.bindIncomeStream(httpIncomeRequestStream);
    httpServerIpc.onMessage(async (request, ipc) => {
      if (request.type !== IPC_DATA_TYPE.REQUEST) {
        return;
      }
      if (request.url === "/" || request.url === "/index.html") {
        /// 收到请求
        httpServerIpc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            200,
            html`
              <!DOCTYPE html>
              <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
                  <meta
                    name="viewport"
                    content="width=device-width, initial-scale=1.0"
                  />
                  <title>Desktop</title>
                </head>
                <body>
                  <h1>你好，这是来自 WebWorker 的响应！</h1>
                  <ol>
                    <li>url:${request.url}</li>
                    <li>method:${request.method}</li>
                    <li>rawHeaders:${JSON.stringify(request.headers)}</li>
                    <li>body:${await request.text()}</li>
                  </ol>
                  <div>
                    <button id="test-readwrite-stream">
                      启动双向信息流测试
                    </button>
                    <pre id="readwrite-stream-log"></pre>
                  </div>
                </body>
                <script type="module" src="./desktop.web.mjs"></script>
              </html>
            `,
            {
              "Content-Type": "text/html",
            }
          )
        );
      } else if (request.url === "/desktop.web.mjs") {
        httpServerIpc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            200,
            script.toString().match(/\{([\w\W]+)\}/)![1],
            {
              "Content-Type": "application/javascript",
            }
          )
        );
      } /* else if (
        request.url === "/readwrite-stream" &&
        request.method === "POST"
      ) {
        const req_stream = await request.stream();
        const res_stream = new ReadableStreamOut<Uint8Array>();
        /// 数据怎么来就怎么回去
        for await (const chunk of streamReader(req_stream)) {
          console.log("qaaq", chunk);
        }
        res_stream.controller.close();
        // streamReader(req_stream).each(
        //   (chunk) => {
        //     console.log("qaaq", chunk);
        //     res_stream.controller.enqueue(chunk);
        //   },
        //   () => res_stream.controller.close()
        // );

        httpServerIpc.postMessage(
          IpcResponse.fromStream(
            request.req_id,
            200,
            res_stream.stream,
            {},
            ipc
          )
        );
      } */
    });
  })();

  console.log("origin", origin);
  {
    const view_id = await process
      .fetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(origin)}`)
      .text();
    // const view_id = await process
    //   .fetch(
    //     `file://mwebview.sys.dweb/open?url=${encodeURIComponent(
    //       `https://localhost.sys.dweb:80`
    //     )}`
    //   )
    //   .string();
  }

  //    addEventListener("fetch", (event) => {
  //       if (event.request.headers["view-id"] === view_id) {
  //       }
  //    });
};
main();
