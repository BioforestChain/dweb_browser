import { IpcHeaders, IpcResponse } from "../../deps.ts";
import {
  $DwebHttpServerOptions,
  $MMID,
  $OnFetchReturn,
  FetchEvent,
  IpcRequest,
  PromiseOut,
  jsProcess,
  mapHelper,
} from "./deps.ts";
import { HttpServer } from "./http-helper.ts";
import { mwebview_destroy } from "./mwebview-helper.ts";
const DNS_PREFIX = "/dns.std.dweb/";
const INTERNAL_PREFIX = "/internal/";

/**给前端的api服务 */
export class Server_api extends HttpServer {
  constructor(public widPo: PromiseOut<string>) {
    super();
  }
  protected _getOptions(): $DwebHttpServerOptions {
    return {
      subdomain: "api",
      port: 443,
    };
  }
  async start() {
    const serverIpc = await this._listener;
    return serverIpc.onFetch(this._provider.bind(this)).internalServerError().cors();
  }

  protected async _provider(event: FetchEvent) {
    // /dns.std.dweb/
    if (event.pathname.startsWith(DNS_PREFIX)) {
      return this._onDns(event);
    } else if (event.pathname.startsWith(INTERNAL_PREFIX)) {
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
        setTimeout(() => {
          jsProcess.restart();
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
      return Response.json({ success: false, message: "no action for serviceWorker Factory !!!" });
    };

    return await result();
  }

  private callbacks = new Map<
    string,
    PromiseOut<{ status: number; headers: Record<string, string>; body: Uint8Array }>
  >();
  protected async _onInternal(event: FetchEvent): Promise<$OnFetchReturn> {
    const pathname = event.pathname.slice(INTERNAL_PREFIX.length);
    if (pathname === "window-info") {
      return Response.json({ wid: await this.widPo.promise });
    }
    if (pathname === "callback") {
      const id = event.searchParams.get("id");
      if (!id) {
        return IpcResponse.fromText(event.req_id, 500, new IpcHeaders(), "invalid search params, miss 'id'", event.ipc);
      }
      const po = mapHelper.getAndRemove(this.callbacks, id);
      if (!po) {
        throw IpcResponse.fromText(event.req_id, 500, new IpcHeaders(), `id:'${id}' unregistered`, event.ipc);
      }
      let status = 200;
      const custom_status = event.searchParams.get("status");
      if (custom_status) {
        status = parseInt(custom_status) || 200;
      }
      const headers = {};
      const custom_headers = event.searchParams.get("headers");
      try {
        if (custom_headers) {
          Object.assign(headers, JSON.parse(custom_headers));
        }
      } catch {}
      po.resolve({
        status,
        headers,
        body: new Uint8Array(await event.arrayBuffer()),
      });
      return Response.json(true);
    }
    if (pathname === "registry-callback") {
      const id = event.searchParams.get("id");
      if (!id) {
        return IpcResponse.fromText(event.req_id, 500, new IpcHeaders(), "invalid search params, miss 'id'", event.ipc);
      }
      if (this.callbacks.has(id)) {
        return IpcResponse.fromText(event.req_id, 500, new IpcHeaders(), `id:'${id}' already registered`, event.ipc);
      }
      const custom_response_po = mapHelper.getOrPut(this.callbacks, id, () => new PromiseOut());
      const custom_response = await custom_response_po.promise;
      return IpcResponse.fromBinary(
        event.req_id,
        custom_response.status,
        new IpcHeaders(custom_response.headers),
        custom_response.body,
        event.ipc
      );
    }
    if (pathname.startsWith("/usr")) {
      const response = await jsProcess.nativeRequest(`file://${pathname}`);
      return new IpcResponse(event.req_id, response.statusCode, response.headers, response.body, event.ipc);
    }
  }

  /**
   * request 事件处理器
   */
  protected async _onApi(
    event: FetchEvent,
    connect = (mmid: $MMID) => jsProcess.connect(mmid),
    useIpcBody = true
  ): Promise<$OnFetchReturn> {
    const { pathname, search } = event;
    // 转发file请求到目标NMM
    const path = `file:/${pathname}${search}`;
    const mmid = new URL(path).host;
    const targetIpc = await connect(mmid as $MMID);
    const ipcProxyRequest = useIpcBody
      ? new IpcRequest(
          //
          targetIpc.allocReqId(),
          path,
          event.method,
          event.headers,
          event.ipcRequest.body,
          targetIpc
        )
      : IpcRequest.fromStream(
          targetIpc.allocReqId(),
          path,
          event.method,
          event.headers,
          await event.ipcRequest.body.stream(),
          targetIpc
        );
    targetIpc.postMessage(ipcProxyRequest);
    const ipcProxyResponse = await targetIpc.registerReqId(ipcProxyRequest.req_id).promise;
    return ipcProxyResponse.toResponse();
  }
}
