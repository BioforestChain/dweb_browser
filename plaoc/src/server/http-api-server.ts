import {
  $DwebHttpServerOptions,
  $Ipc,
  $IpcRequest,
  $IpcResponse,
  IPC_METHOD,
  IpcHeaders,
  IpcRequest,
  IpcResponse,
  jsProcess,
} from "./deps.ts";
import { cros, HttpServer } from "./http-helper.ts";
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
  start() {
    return this._onRequest(this._provider.bind(this));
  }

  protected async _provider(request: $IpcRequest, ipc: $Ipc) {
    if (request.method === IPC_METHOD.OPTIONS) {
      return ipc.postMessage(
        IpcResponse.fromText(
          request.req_id,
          200,
          cros(new IpcHeaders()),
          "",
          ipc
        )
      );
    }
    const url = request.parsed_url;
    // /dns.sys.dweb/
    if (url.pathname.startsWith(DNS_PREFIX)) {
      return this._onDns(request, ipc);
    }
    // /internal/
    else if (url.pathname.startsWith(INTERNAL_PREFIX)) {
      return this._onInternal(request, ipc);
    }
    // /*.dweb
    return this._onApi(request, ipc);
  }

  protected async _onDns(request: $IpcRequest, ipc: $Ipc) {
    const url = new URL(
      "file:/" + request.parsed_url.pathname + request.parsed_url.search
    );
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

    const ipcResponse = IpcResponse.fromText(
      request.req_id,
      200,
      cros(new IpcHeaders()),
      await result(),
      ipc
    );
    // 返回数据到前端
    ipc.postMessage(ipcResponse);
  }

  /**内部请求事件 */
  protected async _onInternal(request: $IpcRequest, ipc: $Ipc) {
    let ipcResponse: undefined | $IpcResponse;
    const href = request.parsed_url.href.replace(INTERNAL_PREFIX, "/");
    const url = new URL(href);
    // 转发public url
    if (url.pathname === "/public-url") {
      const apiServer = await this.getServer();
      const apiHref = apiServer.startResult.urlInfo.buildPublicUrl(
        () => {}
      ).href;
      ipcResponse = IpcResponse.fromText(
        request.req_id,
        200,
        undefined,
        apiHref,
        ipc
      );
    } // 监听属性
    else if (url.pathname === "/observe") {
      const mmid = url.searchParams.get("mmid") as $MMID;
      if (mmid === null) {
        throw new Error("observe require mmid");
      }
      const streamPo = onInternalObserve(mmid);
      ipcResponse = IpcResponse.fromStream(
        request.req_id,
        200,
        undefined,
        streamPo.stream,
        ipc
      );
    }
    if (!ipcResponse) {
      throw new Error(`unknown gateway: ${url.search}`);
    }
    cros(ipcResponse.headers);
    // 返回数据到前端
    ipc.postMessage(ipcResponse);
  }

  /**
   * request 事件处理器
   */
  protected async _onApi(request: $IpcRequest, httpServerIpc: $Ipc) {
    const url = request.parsed_url;
    // 转发file请求到目标NMM
    const path = `file:/${url.pathname}${url.search}`;
    const ipcProxyRequest = new IpcRequest(
      jsProcess.fetchIpc.allocReqId(),
      path,
      request.method,
      request.headers,
      request.body,
      jsProcess.fetchIpc
    );
    // 必须要直接向目标对发连接 通过这个 IPC 发送请求
    const targetIpc = await jsProcess.connect(
      ipcProxyRequest.parsed_url.host as $MMID
    );
    targetIpc.postMessage(ipcProxyRequest);
    const ipcProxyResponse = await targetIpc.registerReqId(
      ipcProxyRequest.req_id
    ).promise;
    const ipcResponse = new IpcResponse(
      request.req_id,
      ipcProxyResponse.statusCode,
      ipcProxyResponse.headers,
      ipcProxyResponse.body,
      httpServerIpc
    );

    cros(ipcResponse.headers);
    // 返回数据到前端
    httpServerIpc.postMessage(ipcResponse);
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
