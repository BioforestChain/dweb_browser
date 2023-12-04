import tls from "node:tls";
import { findPort } from "../../../helper/findPort.ts";
import { createServerCertificate } from "../httpsServerCert.ts";
import { $HttpsServerInfo, httpsCreateServer, NetServer } from "./createNetServer.ts";
import { ProxyServer } from "./ProxyServer.ts";

/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export class HttpsServer extends NetServer<$HttpsServerInfo> {
  static readonly PREFIX = "https://";
  static readonly PROTOCOL = "https:";
  static readonly PORT = 443;
  private _chrome_proxy_clear?: () => unknown;
  private _info?: $HttpsServerInfo;
  get info() {
    return this._info;
  }
  private _proxy = new ProxyServer();

  private bindingPort = -1;
  get authority() {
    return `localhost:${this.bindingPort}`;
  }
  get origin() {
    return `${HttpsServer.PREFIX}${this.authority}`;
  }

  async create() {
    /// 启动一个通用的网关服务
    const local_port = await findPort([(this.bindingPort = 22605)]);
    const info = (this._info = await httpsCreateServer(
      {
        SNICallback: (hostname, callback) => {
          const { pem } = createServerCertificate(hostname);
          callback(null, tls.createSecureContext(pem));
        },
      },
      {
        port: local_port,
      }
    ));

    // info.server.listen(local_port)
    // /// 启动一个通用的代理服务
    // await this._proxy.create({ port: local_port, host: "localhost" });

    return info;
  }
  destroy() {
    this._info?.server.close();
    this._info = undefined;
    return this._proxy.destroy();
  }

  _getHost(subdomain: string, mmid: string, port: number, info: $HttpsServerInfo) {
    return `${subdomain}${mmid}:${port}`;
  }
}
