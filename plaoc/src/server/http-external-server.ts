import { $MicroModuleManifest, isWebSocket } from "../../deps.ts";
import { $MMID } from "../../server.deps.ts";
import {
  $DwebHttpServerOptions,
  $Ipc,
  $IpcRequest,
  $OnFetchReturn,
  $ReadableStreamIpc,
  FetchEvent,
  IPC_ROLE,
  IpcEvent,
  IpcResponse,
  PromiseOut,
  ReadableStreamIpc,
  jsProcess,
  mapHelper,
} from "./deps.ts";
import { HttpServer } from "./http-helper.ts";

declare global {
  interface WindowEventMap {
    fetch: CustomEvent<$IpcRequest>;
  }
}

/**
 * 一种类似开关的 Promise，它有两种状态，我们可以得到当前处于拿个状态，或者等待另外一个状态的切换
 */
class PromiseToggle<T1, T2> {
  constructor(initState: { type: "open"; value: T1 } | { type: "close"; value: T2 }) {
    if (initState.type === "open") {
      this.toggleOpen(initState.value);
    } else {
      this.toggleClose(initState.value);
    }
  }
  private _open = new PromiseOut<T1>();
  private _close = new PromiseOut<T2>();
  waitOpen() {
    return this._open.promise;
  }
  waitClose() {
    return this._close.promise;
  }
  get isOpen() {
    return this._open.is_resolved;
  }
  get isClose() {
    return this._close.is_resolved;
  }
  get openValue() {
    return this._open.value;
  }
  get closeValue() {
    return this._close.value;
  }
  /**
   * 切换到开的状态
   * @param value
   * @returns
   */
  toggleOpen(value: T1) {
    if (this._open.is_resolved) {
      return;
    }
    this._open.resolve(value);
    if (this._close.is_resolved) {
      this._close = new PromiseOut();
    }
  }
  /**
   * 切换到开的状态
   * @param value
   * @returns
   */
  toggleClose(value: T2) {
    if (this._close.is_resolved) {
      return;
    }
    this._close.resolve(value);
    if (this._open.is_resolved) {
      this._open = new PromiseOut();
    }
  }
}

export class Server_external extends HttpServer {
  constructor() {
    super();
    jsProcess.onFetch(async (event) => {
      if (event.pathname == ExternalState.WAIT_EXTERNAL_READY) {
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
      port: 443,
    } satisfies $DwebHttpServerOptions;
  }
  readonly token = crypto.randomUUID();
  async start() {
    const serverIpc = await this._listener;
    return serverIpc.onFetch(this._provider.bind(this)).internalServerError().cors();
  }

  private ipcPo = new PromiseToggle<$ReadableStreamIpc, void>({ type: "close", value: undefined });

  //窗口关闭的时候需要重新等待连接
  closeRegisterIpc() {
    this.ipcPo.toggleClose();
  }

  private externalWaitters = new Map<$MMID, Promise<$Ipc>>();
  // 是否需要激活
  private needActivity = true;
  protected async _provider(event: FetchEvent): Promise<$OnFetchReturn> {
    const { pathname } = event;
    if (pathname.startsWith(`/${this.token}`)) {
      if (isWebSocket(event.method, event.headers)) {
        if (this.ipcPo.isOpen) {
          this.ipcPo.openValue!.close();
          this.ipcPo.toggleClose();
        }
        const streamIpc = new ReadableStreamIpc(
          {
            mmid: jsProcess.mmid,
            name: jsProcess.mmid,
            ipc_support_protocols: {
              cbor: false,
              protobuf: false,
              raw: false,
            },
            dweb_deeplinks: [],
            categories: [],
          } satisfies $MicroModuleManifest,
          IPC_ROLE.SERVER
        );
        this.ipcPo.toggleOpen(streamIpc);
        void streamIpc.bindIncomeStream(event.body!).finally(() => {
          this.ipcPo.toggleClose();
        });

        streamIpc.onFetch(async (event) => {
          const mmid = event.headers.get("mmid") as $MMID;
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
                off1();
              };
              const off1 = ipc.onClose(deleteCache);
            } catch (err) {
              this.externalWaitters.delete(mmid);
              throw err;
            }
            // 激活对面窗口
            ipc.postMessage(IpcEvent.fromText(ExternalState.ACTIVITY, ExternalState.ACTIVITY));
            this.needActivity = false;
            await ipc.request(`file://${mmid}${ExternalState.WAIT_EXTERNAL_READY}`);
            return ipc;
          });
          const ipc = await this.externalWaitters.get(mmid);
          if (ipc && this.needActivity) {
            // 激活对面窗口
            ipc.postMessage(IpcEvent.fromText(ExternalState.ACTIVITY, ExternalState.ACTIVITY));
          }
          const ext_options = this._getOptions();
          // 请求跟外部app通信，并拿到返回值
          event.headers.append("X-Dweb-Host",jsProcess.mmid)
          return await jsProcess.nativeFetch(
            `http://${ext_options.subdomain}.${mmid}:${ext_options.port}${event.pathname}${event.search}`,
            {
              method: event.method,
              headers: event.headers,
              body: event.body,
            }
          );
        });

        /// 返回读写这个stream的链接，注意，目前双工需要客户端通过 WebSocket 来达成支持
        return { body: streamIpc.stream };
      }
      return { status: 500 };
    } else {
      // 接收别人传递过来的消息
      const ipc = await this.ipcPo.waitOpen();
      // 发送到前端监听，并拿去返回值
      const response = (await ipc.request(event.request.url, event.request)).toResponse();
      // ipc.postMessage(response)
      // 构造返回值给对方
      return IpcResponse.fromResponse(event.ipcRequest.req_id, response, event.ipc);
    }
  }
}

// 负责监听对方是否接收了请求
export enum ExternalState {
  ACTIVITY = "activity", // 激活app
  WAIT_CLOSE = "/external-close", // 关闭连接
  WAIT_EXTERNAL_READY = "/wait-external-ready",
}
