import type { $OnFetch, FetchEvent } from "../core/helper/ipcFetchHelper.ts";
import { IPC_METHOD } from "../core/ipc/const.ts";
import { IpcHeaders, IpcResponse } from "../core/ipc/index.ts";

// export class Router {
export class OnFetchAdapter {
  _getMap: Map<string, $OnFetch> = new Map();
  _postMap: Map<string, $OnFetch> = new Map();

  constructor() {}
  private _get(pathname: string, handler: $OnFetch) {
    this._getMap.set(pathname, handler);
    return this;
  }

  private _post(pathname: string, handler: $OnFetch) {
    this._postMap.set(pathname, handler);
    return this;
  }

  add(method: "POST" | "GET", pathname: string, handler: $OnFetch) {
    return method === "GET"
      ? this._get(pathname, handler)
      : this._post(pathname, handler);
  }

  run: $OnFetch = async (event: FetchEvent) => {
    let handler: $OnFetch | undefined;
    switch (event.method) {
      case IPC_METHOD.POST:
        handler = this._postMap.get(event.url.pathname);
        break;

      case IPC_METHOD.GET:
        handler = this._getMap.get(event.url.pathname);
        break;
    }

    return handler === undefined
      ? this._onFetchError(event, 400, `没有匹配请求的路由`)
      : handler(event);
  };

  /**
   * 请求错误的处理器
   * @param event
   * @param statusCode
   * @param errorMessage
   * @returns
   */
  private _onFetchError = async (
    event: FetchEvent,
    statusCode: number,
    errorMessage: string
  ): Promise<IpcResponse> => {
    return IpcResponse.fromJson(
      event.ipcRequest.req_id,
      statusCode,
      new IpcHeaders().init("Content-Type", "application/json"),
      JSON.stringify({
        success: false,
        error: errorMessage,
        data: undefined,
      }),
      event.ipc
    );
  };
}
