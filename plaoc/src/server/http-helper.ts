import {
  $DwebHttpServerOptions,
  $IpcResponse,
  $OnIpcRequestMessage,
  http,
  IpcResponse,
  jsProcess,
} from "./deps.ts";

const { IpcHeaders } = navigator.dweb.ipc;

type $IpcHeaders = InstanceType<typeof IpcHeaders>;

export const cros = (headers: $IpcHeaders) => {
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
  getStartResult() {
    return this._serverP.then((server) => server.startResult);
  }
  async stop() {
    const server = await this._serverP;
    return await server.close();
  }

  private _listener = this.getServer().then((server) => server.listen());

  protected _onRequest(cb: $OnIpcRequestMessage) {
    return this._listener.then((listener) =>
      listener.onRequest(async (request, ipc) => {
        try {
          const result = await cb(request, ipc);
          if (result instanceof IpcResponse) {
            ipc.postMessage(result);
          }
        } catch (err) {
          let err_code = 500;
          let err_message = "";
          let err_detail = "";
          if (err instanceof Error) {
            err_message = err.message;
            err_detail = err.stack ?? err.name;
            if (err instanceof HttpError) {
              err_code = err.code;
            }
          } else {
            err_message = String(err);
          }

          let ipcRepsonse: $IpcResponse;
          if (request.headers.get("Accept") === "application/json") {
            ipcRepsonse = IpcResponse.fromJson(
              request.req_id,
              err_code,
              cros(
                new IpcHeaders().init("Content-Type", "text/html,charset=utf8")
              ),
              { message: err_message, detail: err_detail },
              ipc
            );
          } else {
            ipcRepsonse = IpcResponse.fromText(
              request.req_id,
              err_code,
              cros(
                new IpcHeaders().init("Content-Type", "text/html,charset=utf8")
              ),
              err instanceof Error
                ? `<h1>${err.message}</h1><hr/><pre>${err.stack}</pre>`
                : String(err),
              ipc
            );
          }
          return ipc.postMessage(ipcRepsonse);
        }
      })
    );
  }
}

export class HttpError extends Error {
  constructor(readonly code: number, message: string, options?: ErrorOptions) {
    super(message, options);
  }
}
