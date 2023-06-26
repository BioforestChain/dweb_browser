import {
  $DwebHttpServerOptions,
  $Ipc,
  $IpcRequest,
  $IpcResponse,
  createSignal,
  IpcResponse,
  PromiseOut,
  ReadableStreamOut,
  simpleEncoder,
  u8aConcat,
} from "./deps.ts";
import { cros, HttpError, HttpServer } from "./http-helper.ts";

declare global {
  interface WindowEventMap {
    fetch: CustomEvent<$IpcRequest>;
  }
}

type $OnIpcRequestUrl = (request: $IpcRequest) => void;

export class Server_external extends HttpServer {
  /**
   * 这个token是内部使用的，就作为 特殊的 url.pathname 来处理内部操作
   */
  readonly token = crypto.randomUUID();
  protected _getOptions(): $DwebHttpServerOptions {
    return {
      subdomain: "external",
      port: 443,
    };
  }

  readonly responseMap = new Map<number, PromiseOut<$IpcResponse>>();
  readonly fetchSignal = createSignal<$OnIpcRequestUrl>();

  start() {
    return this._onRequest(this._provider.bind(this));
  }

  protected async _provider(request: $IpcRequest, ipc: $Ipc) {
    const url = request.parsed_url;
    const xHost = decodeURIComponent(url.searchParams.get("X-Dweb-Host") ?? "");

    if (url.pathname === "/" + this.token) {
      /**
       * 这里会处理api的消息返回到前端serviceWorker 构建onFetchEvent 并触发fetch事件
       */
      const action = url.searchParams.get("action");
      if (action === "listen") {
        const streamPo = new ReadableStreamOut<Uint8Array>();
        const ob = { controller: streamPo.controller };
        this.fetchSignal.listen((ipcRequest) => {
          const jsonlineEnd = simpleEncoder("\n", "utf8");
          const json = ipcRequest.toJSON();
          const uint8 = simpleEncoder(JSON.stringify(json), "utf8");
          ob.controller.enqueue(u8aConcat([uint8, jsonlineEnd]));
        });
        return IpcResponse.fromStream(
          request.req_id,
          200,
          undefined,
          streamPo.stream,
          ipc
        );
      }

      // 处理serviceworker respondWith过来的请求,回复给别的app
      if (action === "response") {
        const externalReqId = +(url.searchParams.get("id") ?? "");
        // 验证传递的reqId
        if (isNaN(externalReqId)) {
          throw new HttpError(400, "reqId is NAN");
        }
        const responsePOo = this.responseMap.get(externalReqId);
        // 验证是否有外部请求
        if (!responsePOo) {
          return new HttpError(
            500,
            `not found response by req_id ${externalReqId}`
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
        this.responseMap.delete(externalReqId);
        const icpResponse = IpcResponse.fromText(
          request.req_id,
          200,
          request.headers,
          "ok",
          ipc
        );
        cros(icpResponse.headers);
        // 告知自己的 respondWith 已经发送成功了
        return icpResponse;
      }

      throw new HttpError(502, `unknown action: ${action}`);
    }

    // 别的app发送消息，触发一下前端注册的fetch
    if (xHost === (await this.getStartResult()).urlInfo.host) {
      this.fetchSignal.emit(request);
      const awaitResponse = new PromiseOut<$IpcResponse>();
      this.responseMap.set(request.req_id, awaitResponse);
      const ipcResponse = await awaitResponse.promise;
      cros(ipcResponse.headers);
      // 返回数据到发送者那边
      return ipcResponse;
    }
  }
}
