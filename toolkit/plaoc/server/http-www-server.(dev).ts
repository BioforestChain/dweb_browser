import { X_PLAOC_QUERY } from "./const.ts";
import {
  IpcFetchEvent,
  IpcHeaders,
  jsProcess,
  type $OnFetchReturn,
} from "./deps.ts";
import { Server_www as _Server_www } from "./http-www-server.ts";

/**
 * 给前端的文件服务
 * 这里是开发版，提供了两种额外的功能
 * 1. proxy：将外部http-url替代原有的静态文件，动态加载外部静态文件（主要是代理html，确保域名正确性，script则是用原本的http服务提供）
 */
export class Server_www extends _Server_www {
  private xPlaocProxy: string | null = null;
  override async _provider(request: IpcFetchEvent): Promise<$OnFetchReturn> {
    // 请求申请
    if (
      request.pathname.startsWith(`/${X_PLAOC_QUERY.GET_CONFIG_URL}`) ||
      request.pathname.startsWith("/config.sys.dweb")
    ) {
      return super._provider(request);
    }

    let xPlaocProxy =
      request.searchParams.get(X_PLAOC_QUERY.PROXY) ?? this.xPlaocProxy;
    if (xPlaocProxy === null) {
      const xReferer = request.headers.get("Referer");
      if (xReferer !== null) {
        xPlaocProxy = new URL(xReferer).searchParams.get(X_PLAOC_QUERY.PROXY);
      }
    }
    /// 启用文件模式
    if (xPlaocProxy === null) {
      return super._provider(request);
    }
    this.xPlaocProxy = xPlaocProxy;
    /// 启用跳转模式
    const proxyUrl = new URL(request.pathname + request.search, xPlaocProxy);
    // console.log("native fetch start:", proxyUrl.href);
    const remoteIpcResponse = await jsProcess.nativeRequest(proxyUrl);
    // console.log("native fetch end:", proxyUrl.href,remoteIpcResponse.headers.get("Content-Type"));
    /// 对 html 做强制代理，似的能加入一些特殊的头部信息，确保能正确访问内部的资源
    const contentType = remoteIpcResponse.headers.get("Content-Type");
    const headers = new IpcHeaders(remoteIpcResponse.headers);
    if (
      contentType &&
      (contentType.startsWith("text/html") ||
        !contentType.includes("javascript"))
    ) {
      // 强制声明解除安全性限制
      headers.init("Access-Control-Allow-Private-Network", "true");
      // 移除在iframe中渲染的限制
      headers.delete("X-Frame-Options");
    }

    /// 内容代理转发，这里一般来说不再允许301，允许301的条件太过苛刻，需要远端服务器支持
    return {
      status: remoteIpcResponse.statusCode,
      headers,
      body: remoteIpcResponse.body,
    };
  }
}
