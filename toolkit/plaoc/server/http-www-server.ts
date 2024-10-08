import type { $PlaocConfig } from "./const.ts";
import type { $Core, $Http } from "./deps.ts";
import { IpcResponse, jsProcess } from "./deps.ts";
import { HttpServer } from "./helper/http-helper.ts";
import type { PlaocConfig } from "./plaoc-config.ts";
import { setupDB } from "./shim/db.shim.ts";
import { setupFetch } from "./shim/fetch.shim.ts";

const CONFIG_PREFIX = "/config.sys.dweb/";
/**给前端的文件服务 */
export class Server_www extends HttpServer {
  constructor(readonly plaocConfig: PlaocConfig, private handlers: $Core.$OnFetch[] = []) {
    super("www");
  }
  get jsonPlaoc() {
    return this.plaocConfig.config;
  }
  lang: string | null = null;
  private sessionInfo = jsProcess
    .nativeFetch("file:///usr/sys/session.json")
    .then((res) => res.json() as Promise<{ installTime: number; installUrl: string }>);
  protected _getOptions(): $Http.$DwebHttpServerOptions {
    return {
      subdomain: "www",
    };
  }
  private encoder = new TextEncoder();
  async start() {
    // 设置默认语言
    const lang = await jsProcess.nativeFetch("file://config.sys.dweb/getLang").text();
    if (lang) {
      this.lang = lang;
    } else if (this.jsonPlaoc) {
      this.lang = this.jsonPlaoc.defaultConfig.lang;
    }

    const serverIpc = await this.listen(...this.handlers, this._provider.bind(this));
    return serverIpc.noFound();
  }
  protected async _provider(
    request: $Core.IpcFetchEvent,
    root = this.jsonPlaoc.defaultConfig.wwwRoot ?? "www"
  ): Promise<$Core.$OnFetchReturn> {
    let { pathname } = request;
    // 配置config
    if (pathname.startsWith(CONFIG_PREFIX)) {
      return this._config(request);
    }

    let remoteIpcResponse: $Core.IpcResponse;
    // 进入plaoc转发器
    if (this.jsonPlaoc) {
      const proxyRequest = this._plaocForwarder(request, this.jsonPlaoc);
      pathname = proxyRequest.url.pathname;
      const plaocShims = new Set((proxyRequest.url.searchParams.get("plaoc-shim") ?? "").split(",").filter(Boolean));
      if (plaocShims.has("fetch")) {
        remoteIpcResponse = await jsProcess.nativeRequest(`file:///usr/${root}${pathname}`, {
          headers: proxyRequest.headers,
        });
        const rawText = await remoteIpcResponse.toResponse().text();
        remoteIpcResponse = IpcResponse.fromText(
          remoteIpcResponse.reqId,
          remoteIpcResponse.statusCode,
          remoteIpcResponse.headers,
          `;(${setupFetch.toString()})();${rawText}`,
          remoteIpcResponse.ipc
        );
      } else {
        const sourceResponse = await jsProcess.nativeRequest(`file:///usr/${root}${pathname}?mode=stream`, {
          headers: proxyRequest.headers,
        });
        if (sourceResponse.headers.get("Content-Type")?.includes("text/html") && !plaocShims.has("raw")) {
          const rawText = await sourceResponse.toResponse().text();

          const installTime = (await this.sessionInfo).installTime;
          // 注入setupDB
          const text = `<script>(${setupDB.toString()})(
          "${installTime}",
          ${navigator.dweb?.patch},
          ${navigator.dweb?.brands?.brand});</script>${rawText}`;

          const binary = this.encoder.encode(text);
          // fromBinary 会自动添加正确的 ContentLength, 否则在Safari上会异常
          remoteIpcResponse = IpcResponse.fromBinary(
            request.reqId,
            sourceResponse.statusCode,
            sourceResponse.headers,
            binary,
            sourceResponse.ipc
          );
        } else {
          /**
           * 流转发
           */
          remoteIpcResponse = new IpcResponse(
            request.reqId,
            sourceResponse.statusCode,
            sourceResponse.headers,
            sourceResponse.body,
            request.ipc
          );
        }
      }
    } else {
      const response = await jsProcess.nativeRequest(`file:///usr/${root}${pathname}?mode=stream`);
      /**
       * 流转发，是一种高性能的转发方式，等于没有真正意义上去读取response.body，
       * 而是将response.body的句柄直接转发回去，那么根据协议，一旦流开始被读取，自己就失去了读取权。
       *
       * 如此数据就不会发给我，节省大量传输成本
       */
      remoteIpcResponse = new IpcResponse(
        request.reqId,
        response.statusCode,
        response.headers,
        response.body,
        request.ipc
      );
    }
    return remoteIpcResponse;
  }

  _config(event: $Core.IpcFetchEvent) {
    const pathname = event.pathname.slice(CONFIG_PREFIX.length);
    if (pathname.startsWith("setLang")) {
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
  private _plaocForwarder(request: $Core.IpcFetchEvent, config: $PlaocConfig) {
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
