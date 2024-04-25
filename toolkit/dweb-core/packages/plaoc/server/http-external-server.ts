import type {
  $DwebHttpServerOptions,
  $Ipc,
  $IpcRequest,
  $MMID,
  $OnFetch,
  $OnFetchReturn,
  IpcFetchEvent,
} from "./deps.ts";
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
  constructor(private handlers: $OnFetch[] = []) {
    super("external");
    jsProcess.fetchIpc.onRequest("external").collect(async (event) => {
      if (event.data.parsed_url.pathname == ExternalState.WAIT_EXTERNAL_READY) {
        await this.ipcPo.waitOpen();
      }
      return { status: 200 };
    });
  }
  /**
   * 这个token是内部使用的，就作为 特殊的 url.pathname 来处理内部操作
   */
  protected _getOptions() {
    return {
      subdomain: "external",
    } satisfies $DwebHttpServerOptions;
  }
  readonly token = crypto.randomUUID();
  async start() {
    const serverIpc = await this._listener;
    return serverIpc
      .onFetch(...this.handlers, this._provider.bind(this))
      .internalServerError()
      .cors();
  }

  ipcPo: PromiseToggle<$Ipc, void> = new PromiseToggle<$Ipc, void>({
    type: "close",
    value: undefined,
  });

  //窗口关闭的时候需要重新等待连接
  closeRegisterIpc() {
    this.ipcPo.toggleClose();
  }

  private externalWaitters = new Map<$MMID, Promise<$Ipc>>();
  // 是否需要激活
  private needActivity = true;
  protected async _provider(event: IpcFetchEvent): Promise<$OnFetchReturn> {
    const { pathname } = event;
    // 建立跟自己前端的双工连接
    if (pathname.startsWith(`/${this.token}`)) {
      if (!event.ipcRequest.hasDuplex) {
        return { status: 500 };
      }
      if (this.ipcPo.isOpen) {
        this.ipcPo.openValue!.close();
        this.ipcPo.toggleClose();
      }
      // 跟自己建立双工通信
      const streamIpc = createDuplexIpc(
        jsProcess.ipcPool,
        this._getOptions().subdomain,
        jsProcess.mmid,
        event.ipcRequest,
        () => {
          this.ipcPo.toggleClose();
        }
      );
      this.ipcPo.toggleOpen(streamIpc);

      // 接收前端的externalFetch函数发送的跟外部通信的消息
      streamIpc.onRequest("get-external-fetch").collect(async (event) => {
        const IpcServerRequest = event.data;
        const mmid = IpcServerRequest.headers.get("mmid") as $MMID;
        if (!mmid) {
          return new Response(null, { status: 502 });
        }
        this.needActivity = true;
        await mapHelper.getOrPut(this.externalWaitters, mmid, async (_key) => {
          let ipc: $Ipc;
          try {
            ipc = await jsProcess.connect(mmid);
            const deleteCache = () => {
              this.externalWaitters.delete(mmid);
            };
            ipc.onClosed(deleteCache);
          } catch (err) {
            this.externalWaitters.delete(mmid);
            throw err;
          }
          // 激活对面窗口
          ipc.postMessage(
            IpcEvent.fromText(ExternalState.ACTIVITY, ExternalState.RENDERER)
          );
          this.needActivity = false;
          await ipc.request(
            `file://${mmid}${ExternalState.WAIT_EXTERNAL_READY}`
          );
          return ipc;
        });
        const ipc = await this.externalWaitters.get(mmid);
        if (ipc && this.needActivity) {
          // 激活对面窗口
          ipc.postMessage(
            IpcEvent.fromText(ExternalState.ACTIVITY, ExternalState.RENDERER)
          );
        }
        const ext_options = this._getOptions();
        // 请求跟外部app通信，并拿到返回值
        IpcServerRequest.headers.append("X-External-Dweb-Host", jsProcess.mmid);
        return await jsProcess.nativeFetch(
          `https://${ext_options.subdomain}.${mmid}${IpcServerRequest.parsed_url.pathname}${IpcServerRequest.parsed_url.search}`,
          {
            method: IpcServerRequest.method,
            headers: IpcServerRequest.headers,
            body: await IpcServerRequest.body.stream(),
          }
        );
      });

      /// 返回读写这个stream的链接，注意，目前双工需要客户端通过 WebSocket 来达成支持
      return { status: 101 };
    } else {
      // 接收别人传递过来的消息
      const ipc = await this.ipcPo.waitOpen();
      // 发送到前端监听，并去（respondWith）拿返回值
      const response = (
        await ipc.request(event.request.url, event.request)
      ).toResponse();
      // ipc.postMessage(response)
      // 构造返回值给对方
      return IpcResponse.fromResponse(
        event.ipcRequest.reqId,
        response,
        event.ipc
      );
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
