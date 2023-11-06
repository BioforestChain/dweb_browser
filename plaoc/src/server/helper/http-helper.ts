import { $DwebHttpServerOptions, http, jsProcess, ServerStartResult } from "npm:@dweb-browser/js-process";

const { IpcHeaders } = navigator.dweb.ipc;

type $IpcHeaders = InstanceType<typeof IpcHeaders>;

export const cors = (headers: $IpcHeaders) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*"); // 要支持 X-Dweb-Host
  headers.init("Access-Control-Allow-Methods", "*");
  // headers.init("Connection", "keep-alive");
  // headers.init("Transfer-Encoding", "chunked");
  return headers;
};
export abstract class HttpServer {
  protected abstract _getOptions(): $DwebHttpServerOptions;
  protected _serverP = http.createHttpDwebServer(jsProcess, this._getOptions());
  getServer() {
    return this._serverP;
  }
  async getStartResult():Promise<InstanceType<typeof ServerStartResult>> {
    return this._serverP.then((server) => server.startResult);
  }
  async stop() {
    const server = await this._serverP;
    return await server.close();
  }

  protected _listener = this.getServer().then((server) => server.listen());
}
