import { $OnFetchReturn, FetchEvent, IpcHeaders, jsProcess } from "npm:@dweb-browser/js-process";
import { X_PLAOC_QUERY } from "./const.ts";
import { Server_www as _Server_www } from "./http-www-server.ts";
import { isMobile } from "./shim/is-mobile.ts";

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
    if (isMobile() === false) {
      result.urlInfo.buildExtQuerys.set(X_PLAOC_QUERY.EMULATOR, "*");
    }
    return result;
  }

  protected async _onEmulator(request: FetchEvent, _emulatorFlags: string): Promise<$OnFetchReturn> {
    return super._provider(request, "server/emulator");
  }
  private xPlaocProxy: string | null = null;
  override async _provider(request: FetchEvent): Promise<$OnFetchReturn> {
    // 请求申请
    if (
      request.pathname.startsWith(`/${X_PLAOC_QUERY.GET_CONFIG_URL}`) ||
      request.pathname.startsWith("/config.sys.dweb")
    ) {
      return super._provider(request);
    }

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

    let xPlaocProxy = request.searchParams.get(X_PLAOC_QUERY.PROXY) ?? this.xPlaocProxy;
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
    // console.log("native fetch end:", proxyUrl.href);
    const headers = new IpcHeaders(remoteIpcResponse.headers);
    /// 对 html 做强制代理，似的能加入一些特殊的头部信息，确保能正确访问内部的资源
    if (remoteIpcResponse.headers.get("Content-Type")?.startsWith("text/html")) {
      // 强制声明解除安全性限制
      headers.init("Access-Control-Allow-Private-Network", "true");
      // 移除在iframe中渲染的限制
      headers.delete("X-Frame-Options");
      /// 代理转发
      return {
        status: remoteIpcResponse.statusCode,
        headers,
        body: remoteIpcResponse.body,
      };
    } else {
      return {
        status: 301,
        headers: headers.init("location", proxyUrl.href),
      };
    }
  }
}
