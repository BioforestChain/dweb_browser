import tls from "node:tls";
import { findPort } from "../../../helper/findPort.cjs";
import { createServerCertificate } from "../httpsServerCert.cjs";
import {
  $Http2ServerInfo,
  http2CreateServer,
  NetServer,
} from "./createNetServer.cjs";
import { ProxyServer } from "./ProxyServer.cjs";

/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export class Http2Server extends NetServer<$Http2ServerInfo> {
  private _chrome_proxy_clear?: () => unknown;
  private _info?: $Http2ServerInfo;
  get info() {
    return this._info;
  }
  private _proxy = new ProxyServer();

  async create() {
    /// 启动一个通用的网关服务
    const local_port = await findPort([22605]);
    const info = (this._info = await http2CreateServer(
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

    /// 启动一个通用的代理服务
    await this._proxy.create({ port: local_port, host: "localhost" });

    return info;
  }
  destroy() {
    this._info?.server.close();
    this._info = undefined;
    return this._proxy.destroy();
  }

  _getHost(
    subdomain: string,
    mmid: string,
    port: number,
    info: $Http2ServerInfo
  ) {
    return `${subdomain}${mmid}:${port}`;
  }
}
