import type { $DwebHttpServerOptions, $OnFetch } from "../deps.ts";
import { $once, IpcHeaders, PromiseOut, http, jsProcess } from "../deps.ts";

export type $IpcHeaders = InstanceType<typeof IpcHeaders>;
export { IpcHeaders };

export const cors: (headers: $IpcHeaders) => $IpcHeaders = (headers: $IpcHeaders) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*"); // 要支持 X-Dweb-Host
  headers.init("Access-Control-Allow-Methods", "*");
  // headers.init("Connection", "keep-alive");
  // headers.init("Transfer-Encoding", "chunked");
  return headers;
};

// const serverMap = new Map<string, PromiseOut<HttpDwebServer>>();

export abstract class HttpServer {
  constructor(readonly channelId: string) {}
  private _serverP = PromiseOut.resolve(http.createHttpDwebServer(jsProcess, this._getOptions()));

  protected abstract _getOptions(): $DwebHttpServerOptions;

  getServer() {
    return this._serverP.promise;
  }
  async getStartResult() {
    const server = await this.getServer();
    return server.startResult;
  }
  async stop() {
    const server = await this._serverP.promise;
    return await server.close();
  }

  listen = $once(async (...onFetchs: $OnFetch[]) => {
    const server = await this.getServer();
    return server.listen(...onFetchs);
  });
}
