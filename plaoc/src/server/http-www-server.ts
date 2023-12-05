import { $PlaocConfig } from "./const.ts";
import {
    $DwebHttpServerOptions,
    $OnFetch,
    $OnFetchReturn,
    FetchEvent,
    IpcResponse,
    jsProcess,
} from "./deps.ts";
import { HttpServer, cors } from "./helper/http-helper.ts";
import { PlaocConfig } from "./plaoc-config.ts";
import { setupDB } from "./shim/db.shim.ts";
import { setupFetch } from "./shim/fetch.shim.ts";
import { isMobile } from "./shim/is-mobile.ts";

const CONFIG_PREFIX = "/config.sys.dweb/";
/**给前端的文件服务 */
export class Server_www extends HttpServer {
  constructor(readonly plaocConfig: PlaocConfig, private handlers: $OnFetch[] = []) {
    super();
  }
  get jsonPlaoc() {
    return this.plaocConfig.config;
  }
  lang: string | null = null;
  private sessionInfo = jsProcess
    .nativeFetch("file:///usr/sys/session.json")
    .then((res) => res.json() as Promise<{ installTime: number; installUrl: string }>);
  protected _getOptions(): $DwebHttpServerOptions {
    return {
      subdomain: "www",
    };
  }
  async start() {
    // 设置默认语言
    const lang = await jsProcess.nativeFetch("file://config.sys.dweb/getLang").text();
    if (lang) {
      this.lang = lang;
    } else if (this.jsonPlaoc) {
      this.lang = this.jsonPlaoc.defaultConfig.lang;
    }

    const serverIpc = await this._listener;
    return serverIpc.onFetch(...this.handlers, this._provider.bind(this)).noFound();
  }
  protected async _provider(request: FetchEvent, root = "www"): Promise<$OnFetchReturn> {
    let { pathname } = request;
    // 配置config
    if (pathname.startsWith(CONFIG_PREFIX)) {
      return this._config(request);
    }

    let remoteIpcResponse;
    // 进入plaoc转发器
    if (this.jsonPlaoc) {
      const proxyRequest = await this._plaocForwarder(request, this.jsonPlaoc);
      pathname = proxyRequest.url.pathname;
      const plaocShims = new Set((proxyRequest.url.searchParams.get("plaoc-shim") ?? "").split(",").filter(Boolean));
      if (plaocShims.has("fetch")) {
        remoteIpcResponse = await jsProcess.nativeRequest(`file:///usr/${root}${pathname}`, {
          headers: proxyRequest.headers,
        });
        const rawText = await remoteIpcResponse.toResponse().text();
        remoteIpcResponse = IpcResponse.fromText(
          remoteIpcResponse.req_id,
          remoteIpcResponse.statusCode,
          remoteIpcResponse.headers,
          `;(${setupFetch.toString()})();${rawText}`,
          remoteIpcResponse.ipc
        );
      } else {
        remoteIpcResponse = await jsProcess.nativeRequest(`file:///usr/${root}${pathname}?mode=stream`, {
          headers: proxyRequest.headers,
        });
        if (
          remoteIpcResponse.headers.get("Content-Type")?.includes("text/html") &&
          !plaocShims.has("raw") &&
          isMobile()
        ) {
          const rawText = await remoteIpcResponse.toResponse().text();
          const text = `<script>(${setupDB.toString()})("${(await this.sessionInfo).installTime}");</script>${rawText}`;
          remoteIpcResponse = IpcResponse.fromText(
            remoteIpcResponse.req_id,
            remoteIpcResponse.statusCode,
            remoteIpcResponse.headers,
            text,
            remoteIpcResponse.ipc
          );
        }
      }
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

  async _config(event: FetchEvent) {
    const pathname = event.pathname.slice(CONFIG_PREFIX.length);
    if (pathname.startsWith("/setLang")) {
      const lang = event.searchParams.get("lang");
      this.lang = lang;
    }
    return jsProcess.nativeFetch(`file:/${event.pathname}${event.search}`);
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
        search: redirect.matchUrl.search,
      });
      const pattern = urlPattern.exec(request.url);
      if (!pattern) continue;

      const url = redirect.to.url
        .replace(/\{\{\s*(.*?)\s*\}\}/g, (_exp, match) => {
          const func = new Function("pattern", "lang", "config", `return ${match}`);
          return func(pattern, this.lang, config);
        })
        .replace(/\\/g, "/")
        .replace(/\/\//g, "/");
      const newUrl = new URL(url, request.url);
      request.url.hash = newUrl.hash;
      request.url.host = newUrl.host;
      request.url.hostname = newUrl.hostname;
      request.url.href = newUrl.href;
      request.url.password = newUrl.password;
      request.url.pathname = newUrl.pathname;
      request.url.port = newUrl.port;
      request.url.protocol = newUrl.protocol;
      request.url.search = newUrl.search;
      request.url.username = newUrl.username;
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
