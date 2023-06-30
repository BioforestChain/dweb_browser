import {
  $Ipc,
  $IpcRequest,
  $IpcResponse,
  IpcHeaders,
  IpcResponse,
  PromiseOut,
  ReadableStreamOut,
  simpleEncoder,
  u8aConcat,
} from "./deps.ts";
import { Server_api as _Server_api } from "./http-api-server.ts";
import { cros } from "./http-helper.ts";
const EMULATOR_PREFIX = "/emulator";
export class Server_api extends _Server_api {
  readonly streamMap = new Map<string, ReadableStreamOut<Uint8Array>>();
  readonly responseMap = new Map<number, PromiseOut<$IpcResponse>>();
  readonly jsonlineEnd = simpleEncoder("\n", "utf8");

  protected override async _onApi(request: $IpcRequest, ipc: $Ipc) {
    const url = request.parsed_url;
    console.log(
      url.searchParams.get("mmid"),
      url.searchParams.get("action"),
      request.parsed_url.pathname
    );

    if (request.parsed_url.pathname.startsWith(EMULATOR_PREFIX)) {
      const action = url.searchParams.get("action");
      // 跟emulator组件绑定流
      if (action === "connect") {
        const mmid = url.searchParams.get("mmid");
        if (!mmid) {
          return ipc.postMessage(
            IpcResponse.fromText(
              request.req_id,
              401,
              cros(new IpcHeaders()),
              `key not found in searchParams`,
              ipc
            )
          );
        }
        const streamPo = new ReadableStreamOut<Uint8Array>();
        this.streamMap.set(mmid, streamPo);
        const ipcResponse = IpcResponse.fromStream(
          request.req_id,
          200,
          cros(new IpcHeaders()),
          streamPo.stream,
          ipc
        );
        // 返回数据到前端
        return ipc.postMessage(ipcResponse);
      }
      // 回复真实的request请求到plaoc前端
      if (action === "response") {
        const reqId = parseInt(url.searchParams.get("reqId") ?? "");
        const responsePo = this.responseMap.get(reqId);
        if (!responsePo || isNaN(reqId)) {
          return ipc.postMessage(
            IpcResponse.fromText(
              request.req_id,
              401,
              cros(new IpcHeaders()),
              `reqId=${reqId} cannot be parsed`,
              ipc
            )
          );
        }
        // 转发给外部的app
        responsePo.resolve(
          new IpcResponse(reqId, 200, cros(request.headers), request.body, ipc)
        );
        this.responseMap.delete(reqId);
        return ipc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            200,
            cros(request.headers),
            "ok",
            ipc
          )
        );
      }
      return ipc.postMessage(
        IpcResponse.fromText(
          request.req_id,
          503,
          cros(new IpcHeaders()),
          `request not handle`,
          ipc
        )
      );
    }
    // 真实的request请求
    const mmid = new URL(`file:/${url.pathname}`).host;
    const stream = this.streamMap.get(mmid);
    if (stream) {
      const json = request.toJSON();
      const uint8 = simpleEncoder(JSON.stringify(json), "utf8");
      console.log(JSON.stringify(json));
      // 数据推送到模拟组件那边
      stream.controller.enqueue(u8aConcat([uint8, this.jsonlineEnd]));
      // 创建响应等待
      const awaitResponse = new PromiseOut<$IpcResponse>();
      this.responseMap.set(request.req_id, awaitResponse);
      // 等待 action=response 的返回
      const ipcResponse = await awaitResponse.promise;
      cros(ipcResponse.headers);
      // 返回数据到发送者那边
      return ipc.postMessage(ipcResponse);
    } else {
      super._onApi(request, ipc);
    }
  }
}
