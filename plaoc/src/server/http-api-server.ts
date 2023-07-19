import {
  $DwebHttpServerOptions,
  $Ipc,
  $OnFetchReturn,
  FetchEvent,
  IpcRequest,
  jsProcess,
} from "./deps.ts";
import { HttpServer } from "./http-helper.ts";
export const INTERNAL_PREFIX = "/internal/";
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
      .cors();
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
  protected async _onInternal(
    event: FetchEvent,
    connect = (mmid: $MMID) => jsProcess.connect(mmid)
  ): Promise<$OnFetchReturn> {
    const href = event.url.href.replace(INTERNAL_PREFIX, "/");
    const url = new URL(href);
    // 转发public url
    // if (url.pathname === "/public-url") {
    //   const startResult = await this.getStartResult();
    //   const apiHref = startResult.urlInfo.buildPublicUrl((url) => {
    //     const sessionId = event.searchParams.get(X_PLAOC_QUERY.SESSION_ID);
    //     if (sessionId !== null) {
    //       url.searchParams.set(X_PLAOC_QUERY.SESSION_ID, sessionId);
    //     }
    //   }).href;
    //   return new Response(apiHref);
    // } else 
     // 监听属性
    if (url.pathname === "/observe") {
      const mmid = url.searchParams.get("mmid") as $MMID;
      if (mmid === null) {
        throw new Error("observe require mmid");
      }
      const streamPo = onInternalObserve(mmid, connect);
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
    const mmid = new URL(path).host;
    const targetIpc = await connect(mmid as $MMID);
    const ipcProxyRequest = IpcRequest.fromStream(
      targetIpc.allocReqId(),
      path,
      event.method,
      event.headers,
      body,
      targetIpc
    );

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
  PromiseOut,
  ReadableStreamOut,
  mapHelper,
  simpleEncoder,
  u8aConcat,
} from "./deps.ts";
import { mwebview_destroy } from "./mwebview-helper.ts";

const ipcObserversMap = new Map<$MMID, IpcObserver>();

class IpcObserver {
  readonly ipc = new PromiseOut<$Ipc>();
  readonly obs = new Set<{
    controller: ReadableStreamDefaultController<Uint8Array>;
  }>();
}

/**监听属性的变化 */
const onInternalObserve = (
  mmid: $MMID,
  connect = (mmid: $MMID) => jsProcess.connect(mmid)
) => {
  const streamPo = new ReadableStreamOut<Uint8Array>();
  const observers = mapHelper.getOrPut(ipcObserversMap, mmid, (mmid) => {
    const result = new IpcObserver();
    result.ipc.resolve(connect(mmid));
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
