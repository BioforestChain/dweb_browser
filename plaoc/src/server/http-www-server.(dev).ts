import isMobile from "npm:is-mobile";
import { X_PLAOC_QUERY } from "./const.ts";
import { $OnFetchReturn, FetchEvent, IpcHeaders, jsProcess } from "./deps.ts";
import { emulatorDuplexs } from "./http-api-server.(dev).ts";
import { Server_www as _Server_www } from "./http-www-server.ts";

/**
 * 给前端的文件服务
 * 这里是开发版，提供了两种额外的功能
 * 1. proxy：将外部http-url替代原有的静态文件，动态加载外部静态文件（主要是代理html，确保域名正确性，script则是用原本的http服务提供）
 * 2. emulator：为client所提供的插件提供模拟
 */
export class Server_www extends _Server_www {
  override async getStartResult() {
    const result = await super.getStartResult();
    // TODO 未来如果有需求，可以用 flags 传入参数来控制这个模拟器的初始化参数
    /// 默认强制启动《emulator模拟器插件》
    if (isMobile.isMobile() === false) {
      result.urlInfo.buildExtQuerys.set(X_PLAOC_QUERY.EMULATOR, "*");
    }
    return result;
  }

  protected async _onEmulator(request: FetchEvent, _emulatorFlags: string): Promise<$OnFetchReturn> {
    const indexUrl = (await super.getStartResult()).urlInfo.buildInternalUrl((url) => {
      url.pathname = request.pathname;
      url.search = request.search;
    });

    if (indexUrl.pathname.endsWith(".html") || indexUrl.pathname.endsWith("/")) {
      /// 判 定SessionId 的唯一性，如果已经被使用，创新一个新的 SessionId 进行跳转
      const sessionId = indexUrl.searchParams.get(X_PLAOC_QUERY.SESSION_ID);
      if (sessionId === null || emulatorDuplexs.has(sessionId)) {
        const newSessionId = crypto.randomUUID();
        const updateUrlWithSessionId = (url: URL) => {
          url.searchParams.set(X_PLAOC_QUERY.SESSION_ID, newSessionId);
          return url;
        };
        updateUrlWithSessionId(indexUrl);
        indexUrl.searchParams.set(
          X_PLAOC_QUERY.API_INTERNAL_URL,
          updateUrlWithSessionId(new URL(indexUrl.searchParams.get(X_PLAOC_QUERY.API_INTERNAL_URL)!)).href
        );
        indexUrl.searchParams.set(
          X_PLAOC_QUERY.API_PUBLIC_URL,
          updateUrlWithSessionId(new URL(indexUrl.searchParams.get(X_PLAOC_QUERY.API_PUBLIC_URL)!)).href
        );
        return {
          status: 301,
          headers: {
            Location: indexUrl.href,
          },
        };
      }
    }

    return super._provider(request, "server/emulator");
  }
  override async _provider(request: FetchEvent): Promise<$OnFetchReturn> {
    let isEnableEmulator = request.searchParams.get(X_PLAOC_QUERY.EMULATOR);
    if (isEnableEmulator === null) {
      const ref = request.headers.get("referer");
      if (ref !== null) {
        isEnableEmulator = new URL(ref).searchParams.get(X_PLAOC_QUERY.EMULATOR);
      }
    }
    /// 加载模拟器的外部框架
    if (isEnableEmulator !== null) {
      return this._onEmulator(request, isEnableEmulator);
    }

    let xPlaocProxy = request.searchParams.get(X_PLAOC_QUERY.PROXY);
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
    /// 启用跳转模式
    const proxyUrl = new URL(request.pathname + request.search, xPlaocProxy);
    console.log("native fetch start:", proxyUrl.href);
    const remoteIpcResponse = await jsProcess.nativeFetch(proxyUrl);
    console.log("native fetch end:", proxyUrl.href);
    const headers = new IpcHeaders(remoteIpcResponse.headers);
    /// 对 html 做强制代理，似的能加入一些特殊的头部信息，确保能正确访问内部的资源
    if (remoteIpcResponse.headers.get("Content-Type") === "text/html") {
      // 强制声明解除安全性限制
      headers.init("Access-Control-Allow-Private-Network", "true");
      // 移除在iframe中渲染的限制
      headers.delete("X-Frame-Options");
      /// 代理转发
      return {
        status: remoteIpcResponse.status,
        headers,
        body: remoteIpcResponse.body,
      };
    } else {
      return {
        status: 301,
        headers: headers.init("location", remoteIpcResponse.url),
      };
    }
  }
}
