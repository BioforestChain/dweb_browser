import { isWebSocket } from "dweb/core/helper/ipcRequestHelper.ts";
import { Ipc, IpcHeaders } from "../../deps.ts";
import { $MMID } from "../client/index.ts";
import {
  $DwebHttpServerOptions,
  $IpcRequest,
  $OnFetchReturn,
  FetchError,
  FetchEvent,
  IpcEvent,
  IpcRequest,
  IpcResponse,
  PromiseOut,
  ReadableStreamOut,
  createSignal,
  jsProcess,
  mapHelper,
  simpleEncoder,
  u8aConcat,
} from "./deps.ts";
import { HttpServer, cors } from "./http-helper.ts";

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

  readonly responseMap = new Map<number, PromiseOut<$IpcRequest>>();
  // 拿到fetch的请求
  readonly fetchSignal = createSignal<$OnIpcRequestUrl>();
  // 等待listen触发
  readonly waitListener = new PromiseOut<boolean>();
  // 连接过的app
  readonly connectMap = new Map<$MMID, PromiseOut<Ipc | undefined>>();

  async start() {
    const serverIpc = await this._listener;
    return serverIpc.onFetch(this._provider.bind(this)).internalServerError().cors();
  }

  protected async _provider(event: FetchEvent): Promise<$OnFetchReturn> {
    const { pathname } = event;
    if (pathname.startsWith(`/${this.token}`)) {
      /**
       * 这里会处理api的消息返回到前端serviceWorker 构建onFetchEvent 并触发fetch事件
       */
      const action = event.searchParams.get("action");
      if (isWebSocket(event.method,event.headers)) {
        const streamPo = new ReadableStreamOut<Uint8Array>();
        const ob = { controller: streamPo.controller };
        this.fetchSignal.listen((ipcRequest) => {
          const jsonlineEnd = simpleEncoder("\n", "utf8");
          const json = ipcRequest.toJSON();
          const uint8 = simpleEncoder(JSON.stringify(json), "utf8");
          ob.controller.enqueue(u8aConcat([uint8, jsonlineEnd]));
        });
        // 等待监听流的建立再通知外部发送请求
        this.waitListener.resolve(true);
        return { body: streamPo.stream };
      }
      // 发送对外请求
      if (action === "request") {
        const mmid = event.searchParams.get("mmid") as $MMID | null;
        if (!mmid) {
          throw new FetchError("mmid must be passed", { status: 400 });
        }
        // 连接需要传递信息的jsMicroModule
        const jsIpc = await jsProcess.connect(mmid);
        jsIpc.postMessage(IpcEvent.fromText(ExternalState.ACTIVITY, ExternalState.CONNECT_AWAIT));
        //  建立连接队列
        const ipcPo = mapHelper.getOrPut(this.connectMap, mmid, () => {
          return new PromiseOut();
        });
        jsIpc.onEvent((event) => {
          if (event.name === ExternalState.CONNECT_OK) {
            ipcPo.resolve(jsIpc);
          }
          // 请求关闭
          if (event.name === ExternalState.CLOSE) {
            ipcPo.resolve(undefined);
          }
          //TODO 这个可以在对方窗口关闭的时候,让下次重新等待
          if (event.name === ExternalState.WINDOW_CLOSE) {
            this.connectMap.set(mmid, new PromiseOut());
          }
        });

        if (!(await ipcPo.promise)) {
          // 我们自己主动关闭了 发送close消息到onActivity告知对方关闭等待
          jsIpc.postMessage(IpcEvent.fromText(ExternalState.ACTIVITY, ExternalState.CLOSE));
          return IpcResponse.fromJson(
            event.req_id,
            200,
            cors(event.headers),
            { success: false, message: `app ${mmid} no communication created！！` },
            event.ipc
          );
        }
        //激活对面的程序
        let pathname = event.searchParams.get("pathname") ?? "";
        // 删除不必要的search
        event.searchParams.delete("mmid");
        event.searchParams.delete("X-Dweb-Host");
        event.searchParams.delete("action");
        event.searchParams.delete("pathname");
        pathname = pathname + event.search;

        const req_id = jsIpc.allocReqId();
        const base = event.ipcRequest.parsed_url.origin;
        const ipcRequest = IpcRequest.fromRequest(req_id, jsIpc, `${base}${pathname}`, {
          method: event.method,
          headers: cors(event.headers),
          body: event.body,
        });
        jsIpc.postMessage(ipcRequest);
        const response = await jsIpc.registerReqId(req_id).promise;
        const ipcResponse = new IpcResponse(
          event.req_id,
          response.statusCode,
          cors(event.headers),
          response.body,
          event.ipc
        );
        // 返回数据到前端
        return ipcResponse;
      }

      // 处理serviceworker respondWith过来的请求,回复给别的app
      if (action === "response") {
        const externalReqId = +(event.searchParams.get("id") ?? "");
        // 验证传递的reqId
        if (isNaN(externalReqId)) {
          throw new FetchError("reqId is NAN", { status: 400 });
        }
        const responsePOo = this.responseMap.get(externalReqId);
        // 验证是否有外部请求
        if (!responsePOo) {
          throw new FetchError(`not found response by req_id ${externalReqId}`, { status: 500 });
        }
        // 转发给外部的app
        console.log("external=>", event.ipcRequest);
        responsePOo.resolve(event.ipcRequest);
        this.responseMap.delete(externalReqId);
        const icpResponse = IpcResponse.fromJson(
          event.req_id,
          200,
          cors(new IpcHeaders()),
          { success: "ok" },
          event.ipc
        );
        cors(icpResponse.headers);
        // 告知自己的 respondWith 已经发送成功了
        return icpResponse;
      }
      // 断开连接
      if (action === "close") {
        const mmid = event.searchParams.get("mmid");
        if (!mmid) {
          throw new FetchError("mmid must be passed", { status: 400 });
        }
        const ipcPo = this.connectMap.get(mmid as $MMID);
        if (!ipcPo) {
          return IpcResponse.fromJson(
            event.req_id,
            400,
            cors(event.headers),
            { success: false, message: `No news from ${mmid}` },
            event.ipc
          );
        }
        // 如果还没关闭，强行关闭
        if (!ipcPo.is_resolved) {
          ipcPo.resolve(undefined);
        }
        const ipc = await ipcPo.promise;
        // 如果成功建立过连接，通知对方关闭
        if (ipc) {
          // 向对方发送关闭消息
          ipc.postMessage(IpcEvent.fromText(ExternalState.ACTIVITY, ExternalState.CLOSE));
          ipc.close();
        }
        this.connectMap.delete(mmid as $MMID);
        return IpcResponse.fromJson(
          event.req_id,
          200,
          cors(event.headers),
          { success: true, message: "ok" },
          event.ipc
        );
      }
      // 检查应用是否安装
      if (action === "check") {
        const mmid = event.searchParams.get("mmid");
        if (!mmid) {
          throw new FetchError("mmid must be passed", { status: 400 });
        }
        // 连接需要传递信息的jsMicroModule
        try {
          await jsProcess.connect(mmid as $MMID);
          return IpcResponse.fromJson(
            event.req_id,
            200,
            cors(event.headers),
            { success: true, message: true },
            event.ipc
          );
        } catch {
          return IpcResponse.fromJson(
            event.req_id,
            200,
            cors(event.headers),
            { success: false, message: false },
            event.ipc
          );
        }
      }
      throw new FetchError(`unknown action: ${action}`, { status: 502 });
    }
  }
}

// 负责监听对方是否接收了请求
export enum ExternalState {
  ACTIVITY = "activity", // 激活app
  CLOSE = "close", // 关闭连接
  CONNECT_AWAIT = "connect_await", // 连接等待
  CONNECT_OK = "connect_ok", // 连接成功
  CONNECT_FLASE = "connect_flase", // 连接关闭
  WINDOW_CLOSE = "window_close", //todo
}
