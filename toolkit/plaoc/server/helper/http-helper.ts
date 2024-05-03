import type { $Core, $Http } from "../deps.ts";
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
  constructor(readonly channelId: string) {
    this.init(channelId);
  }
  private _serverP = new PromiseOut<$Http.HttpDwebServer>();

  // deno-lint-ignore require-await
  async init(channelId: string) {
    const target = `http-server-${channelId}`;
    this._serverP.resolve(http.createHttpDwebServer(jsProcess, this._getOptions(), target));
  }

  protected abstract _getOptions(): $Http.$DwebHttpServerOptions;

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

  listen = $once(async (...onFetchs: $Core.$OnFetch[]) => {
    const server = await this.getServer();
    return server.listen(...onFetchs);
  });
}
