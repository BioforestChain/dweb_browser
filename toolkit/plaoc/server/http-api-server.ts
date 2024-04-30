import type { $DwebHttpServerOptions, $Ipc, $MMID, $OnFetch, $OnFetchReturn } from "./deps.ts";
import {
  IpcClientRequest,
  IpcFetchEvent,
  IpcHeaders,
  IpcResponse,
  PromiseOut,
  ReadableStreamEndpoint,
  jsProcess,
  mapHelper,
} from "./deps.ts";

import { HttpServer } from "./helper/http-helper.ts";
import { close_window, mwebview_destroy } from "./helper/mwebview-helper.ts";
const DNS_PREFIX = "/dns.std.dweb/";
const INTERNAL_PREFIX = "/internal/";

/**给前端的api服务 */
export class Server_api extends HttpServer {
  jsRuntime = jsProcess.bootstrapContext;
  constructor(public getWid: () => Promise<string>, private handlers: $OnFetch[] = []) {
    super("api");
  }
  protected _getOptions(): $DwebHttpServerOptions {
    return {
      subdomain: "api",
    };
  }

  async start() {
    const serverIpc = await this.listen(...this.handlers, this._provider.bind(this));
    return serverIpc.internalServerError().cors();
  }

  protected async _provider(event: IpcFetchEvent) {
    // /dns.std.dweb/
    if (event.pathname.startsWith(DNS_PREFIX)) {
      return this._onDns(event);
    } else if (event.pathname.startsWith(INTERNAL_PREFIX)) {
      return this._onInternal(event);
    }
    // /*.dweb
    return this._onApi(event);
  }

  protected async _onDns(event: IpcFetchEvent): Promise<$OnFetchReturn> {
    const url = new URL("file:/" + event.pathname + event.search);
    const pathname = url.pathname;
    const result = async () => {
      if (pathname === "/restart") {
        // 这里只需要把请求发送过去，因为app已经被关闭，已经无法拿到返回值
        setTimeout(async () => {
          const winId = await this.getWid();
          // 这里面在窗口关闭的时候，会触发dns.close 因此不能等待close_window返回再去关闭
          close_window(winId);
          this.jsRuntime.dns.restart(jsProcess.mmid);
        }, 200);
        return Response.json({ success: true, message: "restart ok" });
      }

      // 只关闭 渲染一个渲染进程 不关闭 service
      if (pathname === "/close") {
        const bool = await mwebview_destroy();
        return Response.json({ success: bool, message: "window close" });
      }
      if (pathname === "/query") {
        const mmid = event.searchParams.get("mmid");
        const res = await jsProcess.nativeFetch(`file://dns.std.dweb/query?app_id=${mmid}`);
        return res;
      }
      return Response.json({
        success: false,
        message: "no action for serviceWorker Factory !!!",
      });
    };

    return await result();
  }

  private callbacks = new Map<string, PromiseOut<$Ipc>>();
  #remote = {
    mmid: "localhost.dweb" as `${string}.dweb`,
    ipc_support_protocols: { cbor: false, protobuf: false, json: false },
    dweb_deeplinks: [],
    categories: [],
    name: "",
  };
  protected async _onInternal(event: IpcFetchEvent): Promise<$OnFetchReturn> {
    const pathname = event.pathname.slice(INTERNAL_PREFIX.length);
    // 返回窗口的操作id给前端
    if (pathname === "window-info") {
      return Response.json({ wid: await this.getWid() });
    }
    if (pathname === "callback") {
      const id = event.searchParams.get("id");
      if (!id) {
        return IpcResponse.fromText(event.reqId, 500, new IpcHeaders(), "invalid search params, miss 'id'", event.ipc);
      }
      const ipc = await mapHelper.getOrPut(this.callbacks, id, () => new PromiseOut<$Ipc>()).promise;
      const response = await ipc.request(event.url.href, event.ipcRequest.toRequest());
      return response.toResponse();
    }
    /// websocket
    if (pathname === "registry-callback") {
      const id = event.searchParams.get("id");
      if (!id) {
        return IpcResponse.fromText(event.reqId, 500, new IpcHeaders(), "invalid search params, miss 'id'", event.ipc);
      }
      const endpoint = new ReadableStreamEndpoint(`${jsProcess.mmid}-api-server`);
      const readableStreamIpc = jsProcess.ipcPool.createIpc(endpoint, 0, this.#remote, this.#remote);

      endpoint.bindIncomeStream(event.request.body!);
      mapHelper.getOrPut(this.callbacks, id, () => new PromiseOut()).resolve(readableStreamIpc);
      return IpcResponse.fromStream(event.reqId, 200, undefined, endpoint.stream, event.ipc);
    }
    if (pathname.startsWith("/usr")) {
      const response = await jsProcess.nativeRequest(`file://${pathname}`);
      return new IpcResponse(event.reqId, response.statusCode, response.headers, response.body, event.ipc);
    }
  }

  /**
   * request 事件处理器
   */
  protected async _onApi(event: IpcFetchEvent): Promise<$OnFetchReturn> {
    const { pathname, search } = event;
    // 转发file请求到目标NMM
    const path = `file:/${pathname}${search}`;
    const mmid = new URL(path).host;
    const targetIpc = await jsProcess.connect(mmid as $MMID);
    const { ipcRequest } = event;
    let ipcProxyRequest = new IpcClientRequest(0, path, event.method, event.headers, ipcRequest.body, targetIpc);
    targetIpc.postMessage(ipcProxyRequest);
    let ipcProxyResponse = await targetIpc.request(ipcProxyRequest);

    /// 尝试申请授权
    if (ipcProxyResponse.statusCode === 401) {
      /// 如果授权成功，那么就重新发起请求
      if (await jsProcess.requestDwebPermissions(await ipcProxyResponse.body.text())) {
        ipcProxyRequest = new IpcClientRequest(0, path, event.method, event.headers, ipcRequest.body, targetIpc);
        ipcProxyResponse = await targetIpc.request(ipcProxyRequest);
      }
    }

    if (ipcRequest.hasDuplex) {
      await ipcRequest.client.enableChannel();
    }
    return ipcProxyResponse.toResponse();
  }
}
