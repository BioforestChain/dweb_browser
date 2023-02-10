import type https from "node:https";
import net from "node:net";
import tls from "node:tls";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.cjs";
import { IPC_ROLE } from "../../core/ipc/const.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import type { $ReqMatcher } from "../../helper/$ReqMatcher.cjs";
import { findPort } from "../../helper/findPort.cjs";
import { setChromeProxy } from "../../helper/setChromeProxy.cjs";
import { defaultErrorResponse } from "./defaultErrorResponse.cjs";
import { httpsCreateServer } from "./httpsCreateServer.cjs";
import { createServerCertificate } from "./httpsServerCert.cjs";
import { PortListener } from "./portListener.cjs";
import type { $GetHostOptions } from "./types.cjs";

interface $Gateway {
  listener: PortListener;
  host: string;
  token: string;
}
/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export class HttpServerNMM extends NativeMicroModule {
  mmid = `http.sys.dweb` as const;
  private _chrome_proxy_clear?: () => unknown;
  private _local_port = 0;
  private _local_server?: https.Server;
  // private _local_files?: Map<string, Uint8Array>;
  private _proxy_port = 0;
  private _proxy_server?: https.Server;
  private _tokenMap = new Map</* token */ string, $Gateway>();
  private _gatewayMap = new Map</* host */ string, $Gateway>();
  /** 如果有需要，可以内部实现这个 key 为 "*" 的 listener 来提供默认服务 */
  private _default_listener_host = "*";
  readonly protocol = "https:";
  readonly protocol_default_port = 443;

  protected async _bootstrap() {
    /// 启动一个通用的网关服务
    const local_port = (this._local_port = await findPort([22605]));
    this._local_server = (
      await httpsCreateServer(
        {
          SNICallback: (hostname, callback) => {
            const { pem } = createServerCertificate(hostname);
            callback(null, tls.createSecureContext(pem));
          },
        },
        {
          port: local_port,
        }
      )
    ).server.on("request", (req, res) => {
      /// 获取 host
      let host = this._default_listener_host;
      if (req.headers.host) {
        host = req.headers.host;
        /// 如果没有端口，补全端口
        if (host.includes(":") === false) {
          if (this.protocol === "https:") {
            host += ":443";
          } else if (this.protocol === "http:") {
            host += ":80";
          }
        }
      }

      /// 在网关中寻址能够处理该 host 的监听者
      const gateway = this._gatewayMap.get(host);
      if (gateway == undefined) {
        return defaultErrorResponse(
          req,
          res,
          502,
          "Bad Gateway",
          "作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应"
        );
      }

      // const gateway_timeout = setTimeout(() => {
      //   if (res.writableLength === 0) {
      //   }
      //   res.write;
      //   res.hasHeader;
      // }, 3e4 /* 30s 没有任何 body 写入的话，认为网关超时 */);
      void gateway.listener.hookHttpRequest(req, res);
    });

    /// 启动一个通用的代理服务
    this._proxy_port = await findPort([22600]);
    this._proxy_server = (
      await httpsCreateServer(createServerCertificate("localhost").pem, {
        port: this._proxy_port,
      })
    ).server.on("connect", (clientRequest, clientSocket, head) => {
      // 连接目标服务器
      const targetSocket = net.connect(local_port, "localhost", () => {
        // 通知客户端已经建立连接
        clientSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");

        // 建立通信隧道，转发数据
        targetSocket.write(head);
        clientSocket.pipe(targetSocket).pipe(clientSocket);
      });
    });

    this._chrome_proxy_clear = await setChromeProxy(this._proxy_port);

    /// 监听 IPC 请求

    this.registerCommonIpcOnMessageHanlder({
      pathname: "/listen",
      matchMode: "full",
      input: { port: "number?", subdomain: "string?" },
      output: { origin: "string", token: "string" },
      hanlder: async (args, ipc) => {
        return await this.listen({ ipc, ...args });
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/unlisten",
      matchMode: "full",
      input: { port: "number?", subdomain: "string?" },
      output: "boolean",
      hanlder: async (args, ipc) => {
        return await this.unlisten({ ipc, ...args });
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      method: "POST",
      pathname: "/on-request",
      matchMode: "full",
      input: { token: "string", routes: "object" },
      output: "object",
      hanlder: async (args, ipc, message) => {
        console.log("收到处理请求的双工通道");
        return this.onRequest(
          args.token,
          message,
          args.routes as $ReqMatcher[]
        );
      },
    });
  }
  protected _shutdown() {
    this._local_server?.close();
    this._local_server = undefined;
    this._proxy_server?.close();
    this._proxy_server = undefined;
    this._chrome_proxy_clear?.();
    // this._local_files = undefined;
  }

  private _getHost(options: $GetHostOptions) {
    const { mmid } = options.ipc.remote;
    const { port = this.protocol_default_port } = options;
    let subdomain = options.subdomain?.trim() ?? "";
    if (subdomain.length > 0 && subdomain.endsWith(".") === false) {
      subdomain = subdomain + ".";
    }
    return { host: `${subdomain}${mmid}:${port}`, port, subdomain }; //.localhost:${this._local_port}
  }

  /** 申请监听，获得一个连接地址 */
  private async listen(hostOptions: $GetHostOptions) {
    const { ipc } = hostOptions;
    const { host, port } = this._getHost(hostOptions);
    if (this._gatewayMap.has(host)) {
      throw new Error(`already in listen with port: ${port}`);
    }
    const listener = new PortListener(ipc, port, host, this.protocol);
    /// ipc 在关闭的时候，自动释放所有的绑定
    listener.onDestroy(
      ipc.onClose(() => {
        this.unlisten(hostOptions);
      })
    );

    const token = Buffer.from(
      crypto.getRandomValues(new Uint8Array(64))
    ).toString("base64url");
    const gateway: $Gateway = { listener, host, token };
    this._tokenMap.set(token, gateway);
    this._gatewayMap.set(host, gateway);
    return { token, origin: listener.origin };
  }

  /** 远端监听请求，将提供一个 ReadableStreamIpc 流 */
  private async onRequest(
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
    void streamIpc.bindIncomeStream(message.stream());

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
  private unlisten(hostOptions: $GetHostOptions) {
    const { host } = this._getHost(hostOptions);

    const gateway = this._gatewayMap.get(host);
    if (gateway === undefined) {
      return false;
    }
    this._tokenMap.delete(gateway.token);
    this._gatewayMap.delete(gateway.host);
    /// 执行销毁
    gateway.listener.destroy();

    return true;
  }
}
