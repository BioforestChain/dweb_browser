import { $DwebHttpServerOptions, $IpcRequest, $IpcResponse, IpcResponse, PromiseOut } from "./deps.ts";
import { fetchSignal } from "./http-api-server.ts";
import { HttpServer, cros } from "./http-helper.ts";

declare global {
  interface WindowEventMap {
    fetch: CustomEvent<$IpcRequest>;
  }
}

export class Server_external extends HttpServer {
  protected _getOptions(): $DwebHttpServerOptions {
    return {
      subdomain: "external",
      port: 443,
    };
  }
  async start() {
    const externalServer = await this._serverP;
    // 别滴app发送到请求走这里发送到前端的DwebServiceWorker fetch
    const externalReadableStreamIpc = await externalServer.listen();

    // 关闭信号
    const EXTERNAL_PREFIX = "/external/";
    const externalMap = new Map<number, PromiseOut<$IpcResponse>>();

    // 提供APP之间通信的方法
    externalReadableStreamIpc.onRequest(async (request, ipc) => {
      const url = request.parsed_url;
      const xHost = decodeURIComponent(
        url.searchParams.get("X-Dweb-Host") ?? ""
      );

      // 处理serviceworker respondWith过来的请求,回复给别的app
      if (url.pathname.startsWith(EXTERNAL_PREFIX)) {
        const pathname = url.pathname.slice(EXTERNAL_PREFIX.length);
        const externalReqId = parseInt(pathname);
        // 验证传递的reqId
        if (typeof externalReqId !== "number" || isNaN(externalReqId)) {
          return ipc.postMessage(
            IpcResponse.fromText(
              request.req_id,
              400,
              request.headers,
              "reqId is NAN",
              ipc
            )
          );
        }
        const responsePOo = externalMap.get(externalReqId);
        // 验证是否有外部请求
        if (!responsePOo) {
          return ipc.postMessage(
            IpcResponse.fromText(
              request.req_id,
              500,
              request.headers,
              `not found external requst,req_id ${externalReqId}`,
              ipc
            )
          );
        }
        // 转发给外部的app
        responsePOo.resolve(
          new IpcResponse(
            externalReqId,
            200,
            request.headers,
            request.body,
            ipc
          )
        );
        externalMap.delete(externalReqId);
        const icpResponse = IpcResponse.fromText(
          request.req_id,
          200,
          request.headers,
          "ok",
          ipc
        );
        cros(icpResponse.headers);
        // 告知自己的 respondWith 已经发送成功了
        return ipc.postMessage(icpResponse);
      }

      // 别的app发送消息，触发一下前端注册的fetch
      if (xHost === externalServer.startResult.urlInfo.host) {
        fetchSignal.emit(request);
        const awaitResponse = new PromiseOut<$IpcResponse>();
        externalMap.set(request.req_id, awaitResponse);
        const ipcResponse = await awaitResponse.promise;
        cros(ipcResponse.headers);
        // 返回数据到发送者那边
        ipc.postMessage(ipcResponse);
      }
    });
  }
}
