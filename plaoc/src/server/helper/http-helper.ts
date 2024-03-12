import {
  $DwebHttpServerOptions,
  $ReadableStreamIpc,
  HttpDwebServer,
  IpcHeaders,
  PromiseOut,
  ServerStartResult,
  http,
  jsProcess,
} from "../deps.ts";

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
  private _serverP = new PromiseOut<HttpDwebServer>();
  constructor(readonly channelId: string) {
    const target = `http-server-${channelId}`;
    this._serverP.resolve(http.createHttpDwebServer(target, jsProcess, this._getOptions(), jsProcess.ipcPool));
    // this._serverP = mapHelper.getOrPut(serverMap, target, () => {
    //   const server = new PromiseOut<HttpDwebServer>();
    //   (async () => {
    //     server.resolve(await http.createHttpDwebServer(target, jsProcess, this._getOptions(), jsProcess.ipcPool));
    //   })();
    //   return server;
    // });
  }

  protected abstract _getOptions(): $DwebHttpServerOptions;

  getServer() {
    return this._serverP.promise;
  }
  async getStartResult(): Promise<InstanceType<typeof ServerStartResult>> {
    return this._serverP.promise
      .then((server) => server.startResult)
      .catch((error) => {
        throw error;
      });
  }
  async stop() {
    const server = await this._serverP.promise;
    return await server.close();
  }

  protected _listener: Promise<$ReadableStreamIpc> = this.getServer().then((server) => server.listen());
}
