// plugins 主程序 用来提供 bundle/plugins.txt的内容服务器
// 这个内容需要插入到 html内部执行，创建 webcomponent

import fsPromises from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.cjs";

export class PluginsNMM extends NativeMicroModule {
  mmid = "plugins.sys.dweb" as const;

  // 必须要通过这样启动才可以
  // jsProcess.fetch(`file://plugins.sys.dweb}`) 主要必须要有最后面的路径
  async _bootstrap() {
    const dwebServer = await createHttpDwebServer(this, {});
    this._after_shutdown_signal.listen(() => dwebServer.close());
    /// 从本地文件夹中读取数据返回，
    /// 如果是Android，则使用 AssetManager API 读取文件数据，并且需要手动绑定 mime 与 statusCode

    // ;(await listen()).onRequest(async (request, ipc) => {
    //   // 监听 http:// 协议的请求
    //   // 通过 fetch("http://plugins.sys.dweb-80.localhost:22605/") 访问的请求会被发送到这个位置
    //   console.log('--------------- plugins.main.cts 通过 onRequest 访问')
    //   if(request.parsed_url.pathname === "/" || request.parsed_url.pathname === "/index.html"){
    //     ipc.postMessage(
    //       await IpcResponse.fromText(
    //         request.req_id,
    //         200,
    //         await reqadHtmlFile(),
    //         new IpcHeaders({
    //           "Content-type": "text/html"
    //         })
    //       )
    //     );
    //     return ;
    //   }

    // });

    // 下面注册的是
    // jsProcess.fetch(`file://plugins.sys.dweb/get}`) 事件监听器
    // 提供 plugins webComponent 的代码文本
    // 是 src/plugins 目录下的文件打包后的内容
    this.registerCommonIpcOnMessageHandler({
      pathname: "/get",
      matchMode: "full",
      input: {},
      output: "number",
      handler: async (args, client_ipc, request) => {
        return IpcResponse.fromText(
          request.req_id,
          200,
          undefined,
          await reqadHtmlFile(),
          client_ipc
        );
      },
    });
  }
  _shutdown() { }
}

// 读取 html 文件
async function reqadHtmlFile() {
  const targetPath = path.resolve(
    process.cwd(),
    "./bundle/plugins.components.js"
  );
  const content = await fsPromises.readFile(targetPath);
  return new TextDecoder().decode(content);
}
