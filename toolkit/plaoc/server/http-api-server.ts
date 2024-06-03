import type { $Core, $Http, $Ipc, $MMID } from "./deps.ts";
import { ChannelEndpoint, IpcHeaders, IpcResponse, PromiseOut, jsProcess, mapHelper } from "./deps.ts";

import { HttpServer } from "./helper/http-helper.ts";
import { mwebview_destroy } from "./helper/mwebview-helper.ts";
const DNS_PREFIX = "/dns.std.dweb/";
const INTERNAL_PREFIX = "/internal/";

/**给前端的api服务 */
export class Server_api extends HttpServer {
  jsRuntime = jsProcess.bootstrapContext;
  constructor(public getWid: () => Promise<string>, private handlers: $Core.$OnFetch[] = []) {
    super("api");
  }
  protected _getOptions(): $Http.$DwebHttpServerOptions {
    return {
      subdomain: "api",
    };
  }

  async start() {
    const serverIpc = await this.listen(...this.handlers, this._provider.bind(this));
    return serverIpc.internalServerError().cors();
  }

  // deno-lint-ignore require-await
  protected async _provider(event: $Core.IpcFetchEvent) {
    // /dns.std.dweb/
    if (event.pathname.startsWith(DNS_PREFIX)) {
      return this._onDns(event);
    } else if (event.pathname.startsWith(INTERNAL_PREFIX)) {
      return this._onInternal(event);
    }
    // /*.dweb
    return this._onApi(event);
  }

  protected async _onDns(event: $Core.IpcFetchEvent): Promise<$Core.$OnFetchReturn> {
    const url = new URL("file:/" + event.pathname + event.search);
    const pathname = url.pathname;
    const result = async () => {
      if (pathname === "/restart") {
        // 这里只需要把请求发送过去，因为app已经被关闭，已经无法拿到返回值
        queueMicrotask(() => {
          this.jsRuntime.dns.restart(jsProcess.mmid);
        });
        return Response.json(true);
      }

      // 只关闭 渲染一个渲染进程 不关闭 service
      if (pathname === "/close") {
        const bool = await mwebview_destroy();
        return Response.json(bool);
      }
      if (pathname === "/query") {
        const mmid = event.searchParams.get("mmid") as $MMID;
        const data = await this.jsRuntime.dns.query(mmid);
        return Response.json(data);
      }
      return Response.json(false);
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
  protected async _onInternal(event: $Core.IpcFetchEvent): Promise<$Core.$OnFetchReturn> {
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
      const response = await ipc.request(event.url.href, event.ipcRequest.toPureClientRequest());
      return response.toResponse();
    }
    /// websocket
    if (pathname === "registry-callback") {
      const id = event.searchParams.get("id");
      if (!id) {
        return IpcResponse.fromText(event.reqId, 500, new IpcHeaders(), "invalid search params, miss 'id'", event.ipc);
      }
      const endpoint = new ChannelEndpoint(`${jsProcess.mmid}-api-server`, event.request.body!);
      const readableStreamIpc = jsProcess.ipcPool.createIpc(endpoint, 0, this.#remote, this.#remote);

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
  protected async _onApi(event: $Core.IpcFetchEvent): Promise<$Core.$OnFetchReturn> {
    const { pathname, search } = event;
    // 转发file请求到目标NMM
    const path = `file:/${pathname}${search}`;
    const mmid = new URL(path).host;
    const targetIpc = await jsProcess.connect(mmid as $MMID);
    const { ipcRequest } = event;

    const req = ipcRequest.toPureClientRequest();

    let ipcProxyResponse = await targetIpc.request(path, req);

    /// 尝试申请授权
    if (ipcProxyResponse.statusCode === 401) {
      /// 如果授权成功，那么就重新发起请求
      if (await jsProcess.requestDwebPermissions(await ipcProxyResponse.body.text())) {
        ipcProxyResponse = await targetIpc.request(path, req);
      }
    }
    return ipcProxyResponse.toResponse();
  }
}
