import { IpcHeaders } from "../../deps.ts";
import { $PlaocConfig, X_PLAOC_QUERY } from "./const.ts";
import { $DwebHttpServerOptions, $OnFetchReturn, FetchEvent, IpcResponse, jsProcess } from "./deps.ts";
import { urlStore } from "./helper/urlStore.ts";
import { HttpServer, cors } from "./http-helper.ts";

/**给前端的文件服务 */
export class Server_www extends HttpServer {
  jsonPlaoc: $PlaocConfig | null = null;

  protected _getOptions(): $DwebHttpServerOptions {
    return {
      subdomain: "www",
      port: 443,
    };
  }
  async start() {
    await this._analyzePlaocConfig();
    const serverIpc = await this._listener;
    return serverIpc.onFetch(this._provider.bind(this)).noFound();
  }
  protected async _provider(request: FetchEvent, root = "www"): Promise<$OnFetchReturn> {
    let { pathname } = request;
    // 前端获取一些配置的param
    if (pathname.startsWith(`/${X_PLAOC_QUERY.GET_CONFIG_URL}`)) {
      const obj = urlStore.get() ?? "";
      return IpcResponse.fromJson(request.req_id, 200, cors(new IpcHeaders()), obj, request.ipc);
    }

    let remoteIpcResponse;
    // 进入plaoc转发器
    if (this.jsonPlaoc) {
      const proxyRequest = await this._plaocForwarder(request, this.jsonPlaoc);
      pathname = proxyRequest.url.pathname;
      remoteIpcResponse = await jsProcess.nativeRequest(`file:///usr/${root}${pathname}?mode=stream`, {
        headers: proxyRequest.headers,
      });
    } else {
      remoteIpcResponse = await jsProcess.nativeRequest(`file:///usr/${root}${pathname}?mode=stream`);
    }
    /**
     * 流转发，是一种高性能的转发方式，等于没有真正意义上去读取response.body，
     * 而是将response.body的句柄直接转发回去，那么根据协议，一旦流开始被读取，自己就失去了读取权。
     *
     * 如此数据就不会发给我，节省大量传输成本
     */
    const ipcResponse = new IpcResponse(
      request.req_id,
      remoteIpcResponse.statusCode,
      cors(remoteIpcResponse.headers),
      remoteIpcResponse.body,
      request.ipc
    );
    return ipcResponse;
  }
  // 解析plaoc.json
  private async _analyzePlaocConfig() {
    try {
      const readPlaoc = await jsProcess.nativeRequest(`file:///usr/www/plaoc.json`);
      this.jsonPlaoc = JSON.parse(await readPlaoc.body.text());
      // deno-lint-ignore no-empty
    } catch {}
  }
  /**
   * 转发plaoc.json请求
   * @param request
   * @param config
   */
  private async _plaocForwarder(request: FetchEvent, config: $PlaocConfig) {
    const redirects = config.redirect;
    for (const redirect of redirects) {
      if (!this._matchMethod(request.method, redirect.matchMethod)) {
        continue;
      }
      const urlPattern = new URLPattern({
        pathname: redirect.matchUrl.pathname,
      });
      const pattern = urlPattern.exec({ pathname: request.pathname });
      if (!pattern)  continue;

      const url = redirect.to.url;
      const matches = url.matchAll(/{{\s*(.*?)\s*}}/g); // {{[\w\W]*?}}
      const lang = config.defaultConfig.lang;
      let pathname = url;
      // 执行表达式
      for (const match of matches) {
        const func = new Function("pattern", "lang", `return ${match[1]}`);
        pathname = pathname.replace(/{{\s*(.*?)\s*}}/, func(pattern, lang));
      }
      request.url.pathname = pathname.replace(/\\/g, "/").replace(/\/\//g,"/");
      // appendHeaders
      const appendHeaders = redirect.to.appendHeaders;
      if (appendHeaders && Object.keys(appendHeaders).length !== 0) {
        for (const header of Object.entries(appendHeaders)) {
          request.headers.append(header[0], header[1]);
        }
      }
      // removeHeaders
      const removeHeaders = redirect.to.removeHeaders;
      if (removeHeaders && Object.keys(removeHeaders).length !== 0) {
        for (const header of Object.keys(removeHeaders)) {
          request.headers.delete(header[0]);
        }
      }
      return request;
    }
    return request;
  }
  /**
   * 匹配 * 和 method
   * @param method
   * @param methods
   * @returns
   */
  private _matchMethod(method: string, methods?: string[]) {
    //如果没有传递matchMethod，默认全匹配
    if (!methods) return true;
    // 如果包含*，全部放行
    if (methods.join().indexOf("*") !== -1) return true;
    for (const me in methods) {
      if (me.toLocaleUpperCase() === method) {
        return true;
      }
    }
    return false;
  }
}
