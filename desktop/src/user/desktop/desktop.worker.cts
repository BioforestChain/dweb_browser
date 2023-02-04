/// <reference path="../../sys/js-process.worker.d.ts"/>

import type { $ReqMatcher } from "../../core/helper.cjs";
import type { $HttpRequestInfo } from "../../sys/localhost.cjs";

console.log("ookkkkk, i'm in worker");

export const main = async () => {
  debugger;
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
  const { origin } = await process
    .fetch(`file://localhost.sys.dweb/listen?port=80`)
    .object<{
      origin: string;
    }>();

  (async () => {
    const html = String.raw;
    for await (const request of process
      .fetch(
        `file://localhost.sys.dweb/request/on?port=80&paths=${encodeURIComponent(
          JSON.stringify([
            {
              pathname: "/",
              matchMode: "prefix",
              method: "GET",
            } satisfies $ReqMatcher,
          ])
        )}`
      )
      .jsonlines<$HttpRequestInfo>()) {
      /// 读取请求体 body 的内容
      let body: undefined | string;
      if (request.method === "POST" || request.method === "PUT") {
        body = await process
          .fetch(
            `file://localhost.sys.dweb/request/body?port=80&http_req_id=${request.http_req_id}`
          )
          .string();
      }
      /// 响应请求
      debugger;
      await fetch(
        `file://localhost.sys.dweb/response/emit?port=80&http_req_id=${
          request.http_req_id
        }&statusCode=200&headers=${encodeURIComponent("{}")}`,
        {
          method: "POST",
          body: html`
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
                  <li>rawHeaders:${request.rawHeaders}</li>
                  ${body ? html`<li>body:${body}</li>` : ""}
                </ol>
              </body>
            </html>
          `,
        }
      );
    }
  })();

  console.log("origin", origin);
  {
    const view_id = await process
      .fetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(origin)}`)
      .string();
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
