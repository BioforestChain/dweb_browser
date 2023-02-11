import net from "node:net";
import { findPort } from "../../../helper/findPort.cjs";
import { setChromeProxy } from "../../../helper/setChromeProxy.cjs";
import { createServerCertificate } from "../httpsServerCert.cjs";
import { $HttpsServerInfo, httpsCreateServer } from "./createNetServer.cjs";
/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export class ProxyServer {
  private _chrome_proxy_clear?: () => unknown;
  private _info?: $HttpsServerInfo;
  get info() {
    return this._info;
  }

  async create(
    /** 代理指向的目标 */
    target: { port: number; host: string }
  ) {
    /// 启动一个通用的代理服务
    const proxy_port = await findPort([22600]);
    const info = (this._info = await httpsCreateServer(
      createServerCertificate("localhost").pem,
      {
        port: proxy_port,
      }
    ));

    info.server.on("connect", (clientRequest, clientSocket, head) => {
      // 连接目标服务器
      const targetSocket = net.connect(target.port, target.host, () => {
        // 通知客户端已经建立连接
        clientSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");

        // 建立通信隧道，转发数据
        targetSocket.write(head);
        clientSocket.pipe(targetSocket).pipe(clientSocket);
      });
    });

    this._chrome_proxy_clear = await setChromeProxy(proxy_port);

    return info;
  }
  destroy() {
    this._info?.server.close();
    this._info = undefined;
    this._chrome_proxy_clear?.();
  }
}
