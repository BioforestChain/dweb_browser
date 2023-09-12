import { Buffer } from "node:buffer";
import crypto from "node:crypto";
import type http from "node:http";
import type { IncomingMessage, OutgoingMessage } from "node:http";
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import type { $ReqMatcher } from "../../core/helper/$ReqMatcher.ts";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.ts";
import type { IpcRequest } from "../../core/ipc/IpcRequest.ts";
import { IPC_ROLE } from "../../core/ipc/const.ts";
import type { Ipc } from "../../core/ipc/ipc.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { nativeFetchAdaptersManager } from "../../sys/dns/nativeFetch.ts";
import { ServerStartResult, ServerUrlInfo } from "./const.ts";
import { defaultErrorResponse } from "./defaultErrorResponse.ts";
import { Http1Server } from "./net/Http1Server.ts";
import type { $DwebHttpServerOptions } from "./net/createNetServer.ts";
import { PortListener } from "./portListener.ts";
import { initWebSocketServer } from "./portListener.ws.ts";
import { WebServerRequest } from "./types.ts";

interface $Gateway {
  listener: PortListener;
  urlInfo: ServerUrlInfo;
  token: string;
}

export interface $Listener {
  (req: IncomingMessage, res: OutgoingMessage): void;
}

/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export class HttpServerNMM extends NativeMicroModule {
  mmid = `http.std.dweb` as const;
  name = "HTTP Server Provider";
  override short_name = "HTTP";
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service];
  private _dwebServer = new Http1Server();

  private _tokenMap = new Map</* token */ string, $Gateway>();
  protected _gatewayMap = new Map</* host */ string, $Gateway>();
  private _info:
    | {
        hostname: string;
        port: number;
        host: string;
        origin: string;
        server: http.Server<typeof IncomingMessage, typeof http.ServerResponse>;
        protocol: {
          prefix: string;
          protocol: string;
          port: number;
        };
      }
    | undefined;

  // private _allRoutes: Map<string, $Listener> = new Map();
  getFullReqUrl = (req: WebServerRequest) => {
    return this._info!.protocol.prefix + (req.headers["host"] ?? this._info!.host) + req.url;
  };
  protected async _bootstrap() {
    // 创建了一个基础的 http 服务器 所有的 http:// 请求会全部会发送到这个地方来处理
    const info = (this._info = await this._dwebServer.create());
    const getFullReqUrl = this.getFullReqUrl;

    initWebSocketServer.call(this, info.server);

    this._info.server.on("request", (req, res) => {
      const host = this.getHostByReq(req.url, Object.entries(req.headers));
      {
        // 在网关中寻址能够处理该 host 的监听者
        const gateway = this._gatewayMap.get(host);
        if (gateway == undefined) {
          console.error("http", `[http-server onRequest 既没分发也没有匹配 gatewaty请求] ${req.url}`);
          return defaultErrorResponse(
            req,
            res,
            502,
            "Bad Gateway",
            "作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应"
          );
        }

        void gateway.listener.hookHttpRequest(req, res, getFullReqUrl(req));
      }
    });

    this.onAfterShutdown(
      nativeFetchAdaptersManager.append(async (fromMM, parsedUrl, requestInit) => {
        const host = this.getHostByReq(parsedUrl, []);
        if (host) {
          // 在网关中寻址能够处理该 host 的监听者
          const gateway = this._gatewayMap.get(host);
          if (gateway) {
            const hasMatch = gateway.listener.findMatchedBind(parsedUrl.pathname, requestInit.method || "GET");
            if (hasMatch === undefined) {
              return new Response("no found", { status: 404 });
            }
            const request = new Request(parsedUrl, requestInit);
            /// 目前body不支持上下文句柄，所以这里转成标准 reponse 来发送
            return (await hasMatch.bind.streamIpc.request(request.url, request)).toResponse();
          }
        }
      })
    );

    /// 监听 IPC 请求 /start
    this.registerCommonIpcOnMessageHandler({
      pathname: "/start",
      matchMode: "full",
      input: { port: "number?", subdomain: "string?" },
      output: "object",
      handler: async (args, ipc) => {
        return await this.start(ipc, args);
      },
    });

    /// 监听 IPC 请求 /close
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close",
      matchMode: "full",
      input: { port: "number?", subdomain: "string?" },
      output: "boolean",
      handler: async (args, ipc) => {
        return await this.close(ipc, args);
      },
    });

    /// 监听 IPC 请求 /listen post
    this.registerCommonIpcOnMessageHandler({
      method: "POST",
      pathname: "/listen",
      matchMode: "full",
      input: { token: "string", routes: "object" },
      output: "object",
      handler: (args, _ipc, message) => {
        return this.listen(args.token, message, args.routes as $ReqMatcher[]);
      },
    });
  }
  protected _shutdown() {
    this._dwebServer.destroy();
  }

  private getServerUrlInfo(ipc: Ipc, options: $DwebHttpServerOptions) {
    const mmid = ipc.remote.mmid;
    const { subdomain: options_subdomain = "", port = 80 } = options;
    const subdomainPrefix =
      options_subdomain === "" || options_subdomain.endsWith(".") ? options_subdomain : `${options_subdomain}.`;
    if (port <= 0 || port >= 65536) {
      throw new Error(`invalid dweb http port: ${port}`);
    }

    const public_origin = this._dwebServer.origin;
    const host = `${subdomainPrefix}${mmid}:${port}`;
    const internal_origin = `http://${subdomainPrefix}${mmid}-${port}.${this._dwebServer.authority}`;
    return new ServerUrlInfo(host, internal_origin, public_origin);
  }

  /** 申请监听，获得一个连接地址 */
  private async start(ipc: Ipc, hostOptions: $DwebHttpServerOptions) {
    const serverUrlInfo = this.getServerUrlInfo(ipc, hostOptions);
    if (this._gatewayMap.has(serverUrlInfo.host)) {
      throw new Error(`already in listen: ${serverUrlInfo.internal_origin}`);
    }
    const listener = new PortListener(ipc, serverUrlInfo.host, serverUrlInfo.internal_origin);
    listener.onDestroy(() => this.close(ipc, hostOptions));
    /// ipc 在关闭的时候，自动释放所有的绑定
    ipc.onClose(() => {
      listener.destroy();
    });
    const token = Buffer.from(crypto.getRandomValues(new Uint8Array(64))).toString();
    const gateway: $Gateway = { listener, urlInfo: serverUrlInfo, token };
    this._tokenMap.set(token, gateway);
    this._gatewayMap.set(serverUrlInfo.host, gateway);
    return new ServerStartResult(token, serverUrlInfo);
  }

  /** 远端监听请求，将提供一个 ReadableStreamIpc 流 */
  private async listen(token: string, message: IpcRequest, routes: $ReqMatcher[]) {
    const gateway = this._tokenMap.get(token);
    if (gateway === undefined) {
      throw new Error(`no gateway with token: ${token}`);
    }

    const streamIpc = new ReadableStreamIpc(gateway.listener.ipc.remote, IPC_ROLE.CLIENT);
    void streamIpc.bindIncomeStream(message.body.stream());
    // 自己nmm销毁的时候，ipc也会被全部销毁
    this.addToIpcSet(streamIpc);
    // 自己创建的，就要自己销毁：这个listener被销毁的时候，streamIpc也要进行销毁
    gateway.listener.onDestroy(() => {
      streamIpc.close();
    });
    streamIpc.onClose(
      gateway.listener.addRouter({
        routes,
        streamIpc,
      })
    );

    return new Response(streamIpc.stream, { status: 200 });
  }
  /**
   * 释放监听
   */
  private async close(ipc: Ipc, hostOptions: $DwebHttpServerOptions) {
    const serverUrlInfo = this.getServerUrlInfo(ipc, hostOptions);

    const gateway = this._gatewayMap.get(serverUrlInfo.host);
    if (gateway === undefined) {
      return false;
    }
    this._tokenMap.delete(gateway.token);
    this._gatewayMap.delete(serverUrlInfo.host);
    /// 执行销毁
    gateway.listener.destroy();

    return true;
  }

  // 获取 host
  protected getHostByReq = (url: string | URL = "/", headers: Iterable<[string, string | string[] | undefined]>) => {
    /// 获取 host
    let header_host: string | null = null;
    let header_x_dweb_host: string | null = null;
    let header_user_agent_host: string | null = null;
    if (typeof url === "string") {
      url = new URL(url, this._dwebServer.origin);
    }
    let query_x_web_host: string | null = url.searchParams.get("X-Dweb-Host");
    let url_host: string | null = null;
    if (url) {
      const hostname = url.hostname;
      if (hostname.endsWith(".dweb")) {
        const port = url.port || (url.protocol === "https:" ? "443" : "80");
        url_host = `${hostname}:${port}`;
      }
    }
    for (const [key, value] of headers) {
      switch (key) {
        case "host":
        case "Host": {
          if (typeof value === "string") {
            header_host = value;
            /// 桌面模式下，我们没有对链接进行拦截，将其转化为 `public_origin?X-Dweb-Host` 这种链接形式 ，因为支持 *.localhost 通配符这种域名
            /// 所以这里只需要将 host 中的信息提取出来
            if (value.endsWith(`.${this._dwebServer.authority}`)) {
              query_x_web_host = value.slice(0, -this._dwebServer.authority.length - 1);
              const portStartIndex = query_x_web_host.lastIndexOf("-");
              query_x_web_host =
                query_x_web_host.slice(0, portStartIndex) + ":" + query_x_web_host.slice(portStartIndex + 1);
            }
          }
          break;
        }
        case "x-dweb-host":
        case "X-Dweb-Host": {
          if (typeof value === "string") {
            header_x_dweb_host = value;
          }
          break;
        }
        case "user-agent":
        case "User-Agent": {
          if (typeof value === "string") {
            const host = value.match(/\sdweb-host\/(.+)\s*/)?.[1];
            if (typeof host === "string") {
              header_user_agent_host = host;
            }
          }
        }
      }
    }

    let host = url_host || query_x_web_host || header_x_dweb_host || header_user_agent_host || header_host;
    if (typeof host === "string" && host.includes(":") === false) {
      host += ":" + this._info?.protocol.port;
    }
    if (typeof host !== "string") {
      /** 如果有需要，可以内部实现这个 key 为 "*" 的 listener 来提供默认服务 */
      host = "*";
    }
    return host;
  };
}
