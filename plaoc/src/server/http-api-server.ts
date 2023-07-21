import { $DwebHttpServerOptions, $MMID, $OnFetchReturn, FetchEvent, IpcRequest, jsProcess } from "./deps.ts";
import { HttpServer } from "./http-helper.ts";
import { mwebview_destroy } from "./mwebview-helper.ts";
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
    return serverIpc.onFetch(this._provider.bind(this)).internalServerError().cors();
  }

  protected async _provider(event: FetchEvent) {
    // /dns.sys.dweb/
    if (event.pathname.startsWith(DNS_PREFIX)) {
      return this._onDns(event);
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
