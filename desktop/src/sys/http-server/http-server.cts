import type https from "node:https";
import net from "node:net";
import tls from "node:tls";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.cjs";
import { IPC_ROLE } from "../../core/ipc/const.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import type { $ReqMatcher } from "../../helper/$ReqMatcher.cjs";
import { findPort } from "../../helper/findPort.cjs";
import { setChromeProxy } from "../../helper/setChromeProxy.cjs";
import { defaultErrorResponse } from "./defaultErrorResponse.cjs";
import { httpsCreateServer } from "./httpsCreateServer.cjs";
import { createServerCertificate } from "./httpsServerCert.cjs";
import { PortListener } from "./portListener.cjs";

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
  private _listenerMap = new Map</* host */ string, PortListener>();
  private _default_listener_host = "*";
  readonly protocol = "https:";

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
      const host = req.headers.host ?? this._default_listener_host;
      const listener = this._listenerMap.get(host);
      if (listener == undefined) {
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
      listener.hookHttpRequest(req, res);
    });

    /// 启动一个通用的代理服务
    this._proxy_port = await findPort([22600]);
    this._proxy_server = (
      await httpsCreateServer(createServerCertificate("localhost").pem, {
        port: this._proxy_port,
      })
    ).server.on("connect", (clientRequest, clientSocket, head) => {
      // 连接目标服务器
      const targetSocket = net.connect(local_port, "127.0.0.1", () => {
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
      input: { port: "number" },
      output: { origin: "string" },
      hanlder: async (args, ipc) => {
        return await this.listen(ipc, args.port);
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/unlisten",
      matchMode: "full",
      input: { port: "number" },
      output: "boolean",
      hanlder: async (args, ipc) => {
        return await this.unlisten(ipc, args.port);
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      method: "POST",
      pathname: "/request/on",
      matchMode: "full",
      input: { port: "number", routes: "object" },
      output: "object",
      hanlder: async (args, ipc, message) => {
        console.log("收到处理请求的双工通道");
        return this.onRequest(ipc, message, args.port, args.routes as any);
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

  private _getHost(port: number, ipc: Ipc) {
    return `${ipc.remote.mmid}.${port}.localhost:${this._local_port}`;
  }

  /** 申请监听，获得一个连接地址 */
  private listen(ipc: Ipc, port: number) {
    const host = this._getHost(port, ipc);
    if (this._listenerMap.has(host)) {
      throw new Error(`already in listen with port: ${port}`);
    }
    const listener = new PortListener(ipc, port, host, this.protocol);
    /// ipc 在关闭的时候，自动释放所有的绑定
    listener.onDestroy(
      ipc.onClose(() => {
        this.unlisten(ipc, port);
      })
    );
    this._listenerMap.set(host, listener);

    return {
      origin: listener.origin,
    };
  }

  /** 远端监听请求，将提供一个 ReadableStreamIpc 流 */
  private async onRequest(
    ipc: Ipc,
    message: IpcRequest,
    port: number,
    routes: $ReqMatcher[]
  ) {
    const host = this._getHost(port, ipc);
    const listener = this._listenerMap.get(host);
    if (listener === undefined) {
      throw new Error(`no listen with port: ${port}`);
    }

    const streamIpc = new ReadableStreamIpc(ipc.remote, IPC_ROLE.CLIENT);
    streamIpc.bindIncomeStream(await message.stream());

    listener.addRouter({
      routes,
      streamIpc,
    });
    return new Response(streamIpc.stream, { status: 200 });
  }
  /**
   * 释放监听
   */
  private unlisten(ipc: Ipc, port: number) {
    const host = this._getHost(port, ipc);

    const binding = this._listenerMap.get(host);
    if (binding === undefined) {
      return false;
    }
    this._listenerMap.delete(host);

    binding.destroy();
    return true;
  }
}
