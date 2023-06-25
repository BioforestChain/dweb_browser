import {
  $DwebHttpServerOptions,
  $Ipc,
  $IpcRequest,
  IpcResponse,
  jsProcess,
} from "./deps.ts";
import { HttpServer, cros } from "./http-helper.ts";

/**给前端的文件服务 */
export class Server_www extends HttpServer {
  protected _getOptions(): $DwebHttpServerOptions {
    return {
      subdomain: "www",
      port: 443,
    };
  }
  async start() {
    const wwwServer = await this._serverP;
    // 文件服务处理
    const wwwReadableStreamIpc = await wwwServer.listen();

    wwwReadableStreamIpc.onRequest(async (request, ipc) => {
      ipc.postMessage(await this._provider(request, ipc));
    });
  }
  protected async _provider(request: $IpcRequest, ipc: $Ipc) {
    let pathname = request.parsed_url.pathname;
    if (pathname === "/") {
      pathname = "/index.html";
    }
    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///usr/www${pathname}?mode=stream`
    );
    /**
     * 流转发，是一种高性能的转发方式，等于没有真正意义上去读取response.body，
     * 而是将response.body的句柄直接转发回去，那么根据协议，一旦流开始被读取，自己就失去了读取权。
     *
     * 如此数据就不会发给我，节省大量传输成本
     */
    const ipcResponse = new IpcResponse(
      request.req_id,
      remoteIpcResponse.statusCode,
      cros(remoteIpcResponse.headers),
      remoteIpcResponse.body,
      ipc
    );
    return ipcResponse;
  }
}
