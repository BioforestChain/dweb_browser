import type { Ipc } from "../core/ipc.cjs";
import { NativeMicroModule } from "../core/micro-module.native.cjs";

import http from "node:http";
import url from "node:url";

/**
 * 这是一个模拟监听本地网络的服务，用来为内部程序提供 https 域名来访问网页的功能
 *
 */
export class LocalhostNMM extends NativeMicroModule {
  mmid = "localhost.sys.dweb" as const;
  private listenMap = new Map<
    /* ipc-id */ number,
    Map</* port */ number, /* origin */ string>
  >();
  async _bootstrap() {
    const port = 18909;
    /// nwjs 拦截到请求后不允许直接构建 Response，即便重定向了也不是 302 重定向。
    /// 所以这里我们直接使用 localhost 作为顶级域名来相应实现相关功能，也就意味着这里需要监听端口
    /// 但在IOS和Android上，也可以听过监听端口来实现类似功能，但所有请求都需要校验
    http
      .createServer(async (req, res) => {
        const html = String.raw;
        let body = "";
        if (req.method === "POST" || req.method === "PUT") {
          body = await new Promise<string>((resolve) => {
            const chunks: Uint8Array[] = [];
            req.on("data", (chunk) => {
              chunks.push(Buffer.from(chunk));
            });
            req.on("end", () => {
              resolve(Buffer.concat(chunks).toString("utf-8"));
            });
          });
        }

        res.end(html`
          <!DOCTYPE html>
          <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta http-equiv="X-UA-Compatible" content="IE=edge" />
              <meta
                name="viewport"
                content="width=device-width, initial-scale=1.0"
              />
              <title>Test</title>
            </head>
            <body>
              <h1>你好</h1>
              <p>
                <b>URL:</b>${new URLSearchParams(
                  url.parse(req.url ?? "/").query ?? ""
                ).get("url")}
              </p>
              <p><b>METHOD:</b>${req.method}</p>
              <p><b>HEADERS:</b>${req.rawHeaders}</p>
              <p><b>BODY:</b>${body}</p>
            </body>
          </html>
        `);
      })
      .listen(port);

    this.registerCommonIpcOnMessageHanlder({
      pathname: "/listen",
      matchMode: "full",
      input: { port: "number" },
      output: { origin: "string" },
      hanlder: async (args, ipc) => {
        return await this.listen(args.port, ipc);
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/unregister",
      matchMode: "full",
      input: { port: "number" },
      output: "boolean",
      hanlder: async (args, ipc) => {
        return await this.unlisten(args.port, ipc);
      },
    });
  }
  _shutdown() {}

  /// 监听
  listen(port: number, ipc: Ipc) {
    const origin = `http://${ipc.module.mmid}.${port}.localhost:${port}`;
    return { origin };
  }
  /// 释放监听
  unlisten(port: number, ipc: Ipc) {
    return true;
  }

  // $Routers: {
  //    "/register": IO<mmid, boolean>;
  //    "/unregister": IO<mmid, boolean>;
  // };
}
