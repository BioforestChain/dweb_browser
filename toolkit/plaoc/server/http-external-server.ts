import { onAllIpc } from "@dweb-browser/core/internal/ipcEventExt.ts";
import type { $Core, $Http, $Ipc, $IpcRequest, $MMID } from "./deps.ts";
import { IpcEvent, IpcResponse, jsProcess, mapHelper } from "./deps.ts";
import { createDuplexIpc } from "./helper/duplexIpc.ts";
import { HttpServer } from "./helper/http-helper.ts";
import { PromiseToggle } from "./helper/promise-toggle.ts";

declare global {
  interface WindowEventMap {
    fetch: CustomEvent<$IpcRequest>;
  }
}

/**
 * 一种类似开关的 Promise，它有两种状态，我们可以得到当前处于拿个状态，或者等待另外一个状态的切换
 */
export class Server_external extends HttpServer {
  constructor(private handlers: $Core.$OnFetch[] = []) {
    super("external");
    onAllIpc(jsProcess, "for-external", (ipc) => {
      ipc.onRequest("get-external").collect(async (requestEvent) => {
        const request = requestEvent.consumeFilter(
          (request) => request.parsed_url.pathname == ExternalState.WAIT_EXTERNAL_READY
        );
        if (request) {
          await this.ipcPo.waitOpen();
          ipc.postMessage(IpcResponse.fromJson(request.reqId, 200, undefined, {}, ipc));
        }
      });
    });
  }
  /**
   * 这个token是内部使用的，就作为 特殊的 url.pathname 来处理内部操作
   */
  protected _getOptions() {
    return {
      subdomain: "external",
    } satisfies $Http.$DwebHttpServerOptions;
  }
  readonly token = crypto.randomUUID();

  async start() {
    const serverIpc = await this.listen(...this.handlers, this._provider.bind(this));
    return serverIpc.internalServerError().cors();
  }

  ipcPo: PromiseToggle<$Ipc, void> = new PromiseToggle<$Ipc, void>({
    type: "close",
    value: undefined,
  });

  //窗口关闭的时候需要重新等待连接
  closeRegisterIpc() {
    this.ipcPo.toggleClose();
  }

  #externalWaitters = new Map<$MMID, Promise<$Ipc>>();
  // 是否需要激活
  #needActivity = true;
  protected async _provider(event: $Core.IpcFetchEvent): Promise<$Core.$OnFetchReturn> {
    const { pathname } = event;
    // 建立跟自己前端的双工连接
    if (pathname.startsWith(`/${this.token}`)) {
      if (!event.ipcRequest.hasDuplex) {
        return { status: 500 };
      }
      if (this.ipcPo.isOpen) {
        this.ipcPo.toggleClose();
      }

      // 跟自己建立双工通信
      const streamIpc = createDuplexIpc(
        jsProcess.ipcPool,
        this._getOptions().subdomain,
        jsProcess.mmid,
        event.ipcRequest
      );
      streamIpc.onClosed(() => {
        this.ipcPo.toggleClose();
      });
      this.ipcPo.toggleOpen(streamIpc);

      // 接收前端的externalFetch函数发送的跟外部通信的消息
      streamIpc.onRequest("get-external-fetch").collect(async (event) => {
        const request = event.data;
        const mmid = request.parsed_url.host as $MMID;
        if (!mmid) {
          return streamIpc.postMessage(
            IpcResponse.fromText(request.reqId, 404, undefined, "not found mmid", streamIpc)
          );
        }
        // 是否需要激活应用
        this.#needActivity = !!request.parsed_url.searchParams.get("activate");
        await mapHelper.getOrPut(this.#externalWaitters, mmid, async () => {
          const ipc = await jsProcess.connect(mmid).catch((err) => {
            this.#externalWaitters.delete(mmid);
            streamIpc.postMessage(IpcResponse.fromText(request.reqId, 502, undefined, err, streamIpc));
            throw err;
          });
          ipc.onClosed(() => {
            this.#externalWaitters.delete(mmid);
          });
          await ipc.request(`file://${mmid}${ExternalState.WAIT_EXTERNAL_READY}`);
          return ipc;
        });
        const ipc = await this.#externalWaitters.get(mmid);
        if (ipc && this.#needActivity) {
          // 激活对面窗口
          ipc.postMessage(IpcEvent.fromText(ExternalState.ACTIVITY, ExternalState.RENDERER));
        }
        const ext_options = this._getOptions();
        // 请求跟外部app通信，并拿到返回值
        request.headers.append("X-External-Dweb-Host", jsProcess.mmid);
        const body = request.method === "GET" || "HEAD" ? null : await request.body.stream();
        const res = await jsProcess.nativeFetch(
          `https://${ext_options.subdomain}.${mmid}${request.parsed_url.pathname}${request.parsed_url.search}`,
          {
            method: request.method,
            headers: request.headers,
            body,
          }
        );
        streamIpc.postMessage(await IpcResponse.fromResponse(request.reqId, res, streamIpc));
      });

      /// 返回读写这个stream的链接，注意，目前双工需要客户端通过 WebSocket 来达成支持
      return { status: 101 };
    } else {
      // 接收别人传递过来的消息
      // 等待自己的ipc连接成功
      const ipc = await this.ipcPo.waitOpen();
      // 发送到前端监听，并去（respondWith）拿返回值
      const response = (await ipc.request(event.request.url, event.request)).toResponse();
      // 构造返回值给对方
      return IpcResponse.fromResponse(event.ipcRequest.reqId, response, event.ipc);
    }
  }
}

// 负责监听对方是否接收了请求
export enum ExternalState {
  ACTIVITY = "activity", // 激活app
  RENDERER = "renderer", // 窗口激活时发出，这里可以拿到应用的窗口句柄（wid）
  WAIT_CLOSE = "/external-close", // 关闭连接
  WAIT_EXTERNAL_READY = "/wait-external-ready",
}
