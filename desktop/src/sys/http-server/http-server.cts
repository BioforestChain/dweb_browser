import crypto from "node:crypto";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.cjs";
import { IPC_ROLE } from "../../core/ipc/const.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import type { $ReqMatcher } from "../../helper/$ReqMatcher.cjs";
import { ServerStartResult, ServerUrlInfo } from "./const.js";
import { defaultErrorResponse } from "./defaultErrorResponse.cjs";
import type { $DwebHttpServerOptions } from "./net/createNetServer.cjs";
import { Http1Server } from "./net/Http1Server.cjs";
import { PortListener } from "./portListener.cjs";
import chalk from "chalk"

interface $Gateway {
  listener: PortListener;
  urlInfo: ServerUrlInfo;
  token: string;
}
/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export class HttpServerNMM extends NativeMicroModule {
  mmid = `http.sys.dweb` as const;
  private _dwebServer = new Http1Server();

  private _tokenMap = new Map</* token */ string, $Gateway>();
  private _gatewayMap = new Map</* host */ string, $Gateway>();

  protected async _bootstrap() {
    const info = await this._dwebServer.create();
    info.server.on("request", (req, res) => {
      // console.log('[http-server.cts 接受到了 http 请求：]', req.headers.host)

      /// 获取 host
      var header_host: string | null = null;
      var header_x_dweb_host: string | null = null;
      var header_user_agent_host: string | null = null;
      var query_x_web_host: string | null = new URL(
        req.url || "/",
        this._dwebServer.origin
      ).searchParams.get("X-Dweb-Host");
      for (const [key, value] of Object.entries(req.headers)) {
        switch (key) {
          case "host":
          case "Host": {
            if (typeof value === "string") {
              header_host = value;
              /// 桌面模式下，我们没有对链接进行拦截，将其转化为 `public_origin?X-Dweb-Host` 这种链接形式 ，因为支持 *.localhost 通配符这种域名
              /// 所以这里只需要将 host 中的信息提取出来
              if (value.endsWith(`.${this._dwebServer.authority}`)) {
                query_x_web_host = value
                  .slice(0, -this._dwebServer.authority.length - 1)
                  .replace(/-(\d+)/, ":$1");
              }
            }
            break;
          }
          case "x-dweb-host":
          case "X-Dweb-Host": {
            if (typeof value === "string") {
              header_x_dweb_host = value;
            }
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

      let host =
        query_x_web_host ||
        header_x_dweb_host ||
        header_user_agent_host ||
        header_host;
      if (typeof host === "string" && host.includes(":") === false) {
        host += ":" + info.protocol.port;
      }
      if (typeof host !== "string") {
        /** 如果有需要，可以内部实现这个 key 为 "*" 的 listener 来提供默认服务 */
        host = "*";
      }

      /// 在网关中寻址能够处理该 host 的监听者
      const gateway = this._gatewayMap.get(host);
      // console.log('[http-server.cts 接受到了 http 请求：gateway]',gateway)
      if (gateway == undefined) {
        console.log(chalk.yellow('[http-server.cts 接受到了没有匹配的 gateway host===]'),host)
        return defaultErrorResponse(
          req,
          res,
          502,
          "Bad Gateway",
          "作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应"
        );
      }
      // gateway.listener.ipc.request("/on-connect")

      // const gateway_timeout = setTimeout(() => {
      //   if (res.writableLength === 0) {
      //   }
      //   res.write;
      //   res.hasHeader;
      // }, 3e4 /* 30s 没有任何 body 写入的话，认为网关超时 */);
      void gateway.listener.hookHttpRequest(req, res);
    });

    /// 监听 IPC 请求
    this.registerCommonIpcOnMessageHandler({
      pathname: "/start",
      matchMode: "full",
      input: { port: "number?", subdomain: "string?" },
      output: "object",
      handler: async (args, ipc) => {
        return await this.start(ipc, args);
      },
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close",
      matchMode: "full",
      input: { port: "number?", subdomain: "string?" },
      output: "boolean",
      handler: async (args, ipc) => {
        return await this.close(ipc, args);
      },
    });
    this.registerCommonIpcOnMessageHandler({
      method: "POST",
      pathname: "/listen",
      matchMode: "full",
      input: { token: "string", routes: "object" },
      output: "object",
      handler: async (args, ipc, message) => {
        console.log("收到处理请求的双工通道");
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
      options_subdomain === "" || options_subdomain.endsWith(".")
        ? options_subdomain
        : `${options_subdomain}.`;
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
      throw new Error(`already in listen: ${origin}`);
    }
    const listener = new PortListener(
      ipc,
      serverUrlInfo.host,
      serverUrlInfo.internal_origin
    );
    /// ipc 在关闭的时候，自动释放所有的绑定
    listener.onDestroy(
      ipc.onClose(() => {
        this.close(ipc, hostOptions);
      })
    );
    // jmmMetadata.sys.dweb-80.localhost:22605
    // jmmmetadata.sys.dweb-80.localhost:22605
    const token = Buffer.from(
      crypto.getRandomValues(new Uint8Array(64))
    ).toString("base64url");
    const gateway: $Gateway = { listener, urlInfo: serverUrlInfo, token };
    this._tokenMap.set(token, gateway);
    this._gatewayMap.set(serverUrlInfo.host, gateway);
    return new ServerStartResult(token, serverUrlInfo);
  }

  /** 远端监听请求，将提供一个 ReadableStreamIpc 流 */
  private async listen(
    token: string,
    message: IpcRequest,
    routes: $ReqMatcher[]
  ) {
    const gateway = this._tokenMap.get(token);
    if (gateway === undefined) {
      throw new Error(`no gateway with token: ${token}`);
    }

    const streamIpc = new ReadableStreamIpc(
      gateway.listener.ipc.remote,
      IPC_ROLE.CLIENT
    );
    void streamIpc.bindIncomeStream(message.body.stream());

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
  private close(ipc: Ipc, hostOptions: $DwebHttpServerOptions) {
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
}
