import type {
  $DwebHttpServerOptions,
  HttpDwebServer
} from "../deps.ts";
import {
  IpcHeaders,
  PromiseOut,
  ServerStartResult,
  http,
  jsProcess
} from "../deps.ts";

export type $IpcHeaders = InstanceType<typeof IpcHeaders>;
export { IpcHeaders };

export const cors: (headers: $IpcHeaders) => $IpcHeaders = (
  headers: $IpcHeaders
) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*"); // 要支持 X-Dweb-Host
  headers.init("Access-Control-Allow-Methods", "*");
  // headers.init("Connection", "keep-alive");
  // headers.init("Transfer-Encoding", "chunked");
  return headers;
};

// const serverMap = new Map<string, PromiseOut<HttpDwebServer>>();

export abstract class HttpServer {
  constructor(readonly channelId: string) {
    this.init(channelId);
  }
  private _serverP = new PromiseOut<HttpDwebServer>();

  async init(channelId: string) {
    const target = `http-server-${channelId}`;
    this._serverP.resolve(
      http.createHttpDwebServer(
        jsProcess,
        this._getOptions(),
      )
    );
  }

  protected abstract _getOptions(): $DwebHttpServerOptions;

  getServer() {
    return this._serverP.promise;
  }
  async getStartResult(): Promise<InstanceType<typeof ServerStartResult>> {
    return this.getServer()
      .then((server) => {
        // console.log("getStartResult success=>", server.startResult.urlInfo.public_origin);
        return server.startResult;
      })
      .catch((error) => {
        // console.log("getStartResult error=>", error);
        throw error;
      });
  }
  async stop() {
    const server = await this._serverP.promise;
    return await server.close();
  }

  protected _listener = this.getServer()
    .then(async (server) => {
      const ipc = await server.listen();
      // console.log("创建服务=>", ipc.channelId);
      return ipc;
    })
    .catch((err) => {
      throw err;
    });
}
