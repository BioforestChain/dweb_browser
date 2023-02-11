import { findPort } from "../../../helper/findPort.cjs";
import {
  $HttpServerInfo,
  httpCreateServer,
  NetServer,
} from "./createNetServer.cjs";

/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export class Http1Server extends NetServer<$HttpServerInfo> {
  private _info?: $HttpServerInfo;
  get info() {
    return this._info;
  }

  async create() {
    /// 启动一个通用的网关服务
    const local_port = await findPort([22605]);
    return (this._info = await httpCreateServer(
      {},
      {
        port: local_port,
      }
    ));
  }
  destroy() {
    this._info?.server.close();
    this._info = undefined;
  }

  _getHost(
    subdomain: string,
    mmid: string,
    port: number,
    info: $HttpServerInfo
  ): string {
    return `${subdomain}${mmid}-${port}.localhost:${info.port}`;
  }
}
