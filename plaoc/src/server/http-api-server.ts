import {
  $DwebHttpServerOptions,
  $Ipc,
  $OnFetchReturn,
  FetchEvent,
  IpcRequest,
  jsProcess,
} from "./deps.ts";
import { HttpServer } from "./http-helper.ts";
const INTERNAL_PREFIX = "/internal/";
const DNS_PREFIX = "/dns.sys.dweb/";

/**给前端的api服务 */
export class Server_api extends HttpServer {
  protected _getOptions(): $DwebHttpServerOptions {
    return {
      subdomain: "api",
      port: 443,
    };
  }
  async start() {
    const serverIpc = await this._listener;
    return serverIpc
      .onFetch(this._provider.bind(this))
      .internalServerError()
      .cros();
  }

  protected async _provider(event: FetchEvent) {
    // /dns.sys.dweb/
    if (event.pathname.startsWith(DNS_PREFIX)) {
      return this._onDns(event);
    }
    // /internal/
    else if (event.pathname.startsWith(INTERNAL_PREFIX)) {
      return this._onInternal(event);
    }
    // /*.dweb
    return this._onApi(event);
  }

  protected async _onDns(event: FetchEvent): Promise<$OnFetchReturn> {
    const url = new URL("file:/" + event.pathname + event.search);
    const pathname = url.pathname;
    const result = async () => {
      if (pathname === "/restart") {
        // 这里只需要把请求发送过去，因为app已经被关闭，已经无法拿到返回值
        jsProcess.restart();
        return "restart ok";
      }

      // 只关闭 渲染一个渲染进程 不关闭 service
      if (pathname === "/close") {
        mwebview_destroy();
        return "window close";
      }
      return "no action for serviceWorker Factory !!!";
    };

    return new Response(await result());
  }

  /**内部请求事件 */
  protected async _onInternal(event: FetchEvent): Promise<$OnFetchReturn> {
    const href = event.url.href.replace(INTERNAL_PREFIX, "/");
    const url = new URL(href);
    // 转发public url
    if (url.pathname === "/public-url") {
      const startResult = await this.getStartResult();
      const apiHref = startResult.urlInfo.buildPublicUrl().href;
      return new Response(apiHref);
    } // 监听属性
    else if (url.pathname === "/observe") {
      const mmid = url.searchParams.get("mmid") as $MMID;
      if (mmid === null) {
        throw new Error("observe require mmid");
      }
      const streamPo = onInternalObserve(mmid);
      return new Response(streamPo.stream);
    }
  }

  /**
   * request 事件处理器
   */
  protected async _onApi(
    event: FetchEvent,
    connect = (mmid: $MMID) => jsProcess.connect(mmid)
  ): Promise<$OnFetchReturn> {
    const { pathname, search } = event;
    // 转发file请求到目标NMM
    const path = `file:/${pathname}${search}`;
    const body = await event.ipcRequest.body.stream();
    const ipcProxyRequest = body
      ? IpcRequest.fromStream(
          jsProcess.fetchIpc.allocReqId(),
          path,
          event.method,
          event.headers,
          body,
          jsProcess.fetchIpc
        )
      : IpcRequest.fromText(
          jsProcess.fetchIpc.allocReqId(),
          path,
          event.method,
          event.headers,
          "",
          jsProcess.fetchIpc
        );
    // 必须要直接向目标对发连接 通过这个 IPC 发送请求
    const targetIpc = await connect(ipcProxyRequest.parsed_url.host as $MMID);

    targetIpc.postMessage(ipcProxyRequest);
    const ipcProxyResponse = await targetIpc.registerReqId(
      ipcProxyRequest.req_id
    ).promise;
    return ipcProxyResponse.toResponse();
  }
}

import { OBSERVE } from "./const.ts";
import {
  $MMID,
  mapHelper,
  PromiseOut,
  ReadableStreamOut,
  simpleEncoder,
  u8aConcat,
} from "./deps.ts";
import { mwebview_destroy } from "./mwebview-helper.ts";

const ipcObserversMap = new Map<
  $MMID,
  {
    ipc: PromiseOut<$Ipc>;
    obs: Set<{ controller: ReadableStreamDefaultController<Uint8Array> }>;
  }
>();

/**监听属性的变化 */
const onInternalObserve = (mmid: $MMID) => {
  const streamPo = new ReadableStreamOut<Uint8Array>();
  const observers = mapHelper.getOrPut(ipcObserversMap, mmid, (mmid) => {
    const result = { ipc: new PromiseOut<$Ipc>(), obs: new Set() };
    result.ipc.resolve(jsProcess.connect(mmid));
    result.ipc.promise.then((ipc) => {
      ipc.onEvent((event) => {
        if (event.name !== OBSERVE.State) {
          return;
        }
        const observers = ipcObserversMap.get(ipc.remote.mmid);
        const jsonlineEnd = simpleEncoder("\n", "utf8");
        if (observers && observers.obs.size > 0) {
          for (const ob of observers.obs) {
            ob.controller.enqueue(u8aConcat([event.binary, jsonlineEnd]));
          }
        }
      });
    });
    return result;
  });
  const ob = { controller: streamPo.controller };
  observers.obs.add(ob);
  streamPo.onCancel(() => {
    observers.obs.delete(ob);
  });
  return streamPo;
};
