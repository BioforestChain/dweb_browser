import { X_PLAOC_QUERY } from "./const.ts";
import {
  $Ipc,
  $IpcRequest,
  IpcHeaders,
  IpcResponse,
  jsProcess,
} from "./deps.ts";
import { cros } from "./http-helper.ts";
import { Server_www as _Server_www } from "./http-www-server.ts";

/**
 * 给前端的文件服务
 * 这里是开发版，提供了两种额外的功能
 * 1. proxy：将外部http-url替代原有的静态文件，动态加载外部静态文件（主要是代理html，确保域名正确性，script则是用原本的http服务提供）
 * 2. emulator：为client所提供的插件提供模拟
 */
export class Server_www extends _Server_www {
  async getStartResult() {
    const result = await super.getStartResult();
    // TODO 未来如果有需求，可以用 flags 传入参数来控制这个模拟器的初始化参数
    /// 默认强制启动《emulator模拟器插件》
    result.urlInfo.buildExtQuerys.set(X_PLAOC_QUERY.EMULATOR, "*");
    return result;
  }
  override async _provider(request: $IpcRequest, ipc: $Ipc) {
    const isEnableEmulator = request.parsed_url.searchParams.get(
      X_PLAOC_QUERY.EMULATOR
    );

    /// 加载模拟器的外部框架
    if (isEnableEmulator !== null) {
      const html = String.raw;
      const emulatorJsResponse = await jsProcess.nativeRequest(
        `file:///usr/server/plaoc.emulator.js`
      );
      const indexUrl = new URL(request.parsed_url);
      indexUrl.searchParams.delete(X_PLAOC_QUERY.EMULATOR);
      return IpcResponse.fromText(
        request.req_id,
        200,
        new IpcHeaders().init("Content-Type", "text/html"),
        html`
          <!DOCTYPE html>
          <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta
                name="viewport"
                content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0"
              />
              <title>Plaoc-Emulator</title>
              <style>
                html,
                body,
                root-comp {
                  width: 100%;
                  height: 100%;
                  margin: 0;
                  padding: 0;
                  overflow: hidden;
                }
              </style>
              <script>
                ${await emulatorJsResponse.body.text()};
              </script>
            </head>
            <body>
              <root-comp>
                <iframe
                  style="width:100%;height:100%;border:0;"
                  slot="app-content"
                  src="${indexUrl.href}"
                ></iframe>
              </root-comp>
            </body>
          </html>
        `,
        ipc
      );
    }

    let xPlaocProxy = request.parsed_url.searchParams.get(X_PLAOC_QUERY.PROXY);
    if (xPlaocProxy === null) {
      const xReferer = request.headers.get("Referer");
      if (xReferer !== null) {
        xPlaocProxy = new URL(xReferer).searchParams.get(X_PLAOC_QUERY.PROXY);
      }
    }
    if (xPlaocProxy === null) {
      return super._provider(request, ipc);
    }

    const remoteIpcResponse = await fetch(
      new URL(request.parsed_url.pathname, xPlaocProxy)
    );
    const headers = new IpcHeaders(remoteIpcResponse.headers);
    /// 对 html 做强制代理，似的能加入一些特殊的头部信息，确保能正确访问内部的资源
    if (remoteIpcResponse.headers.get("Content-Type") === "text/html") {
      // 强制声明解除安全性限制
      headers.init("Access-Control-Allow-Private-Network", "true");
      // 移除在iframe中渲染的限制
      headers.delete("X-Frame-Options");
      return IpcResponse.fromStream(
        request.req_id,
        remoteIpcResponse.status,
        headers,
        remoteIpcResponse.body!,
        ipc
      );
    } else {
      headers.init("location", remoteIpcResponse.url);
      return IpcResponse.fromText(request.req_id, 301, cros(headers), "", ipc);
    }
  }
}
