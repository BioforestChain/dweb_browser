import type { $OnFetch, FetchEvent } from "../../core/helper/ipcFetchHelper.ts";
import { IPC_METHOD } from "../../core/ipc/const.ts";
import { IpcHeaders, IpcResponse } from "../../core/ipc/index.ts";

export class Router {
  _getMap: Map<string, $OnFetch> = new Map();
  _postMap: Map<string, $OnFetch> = new Map();

  constructor() {}
  get(pathname: string, handler: $OnFetch) {
    this._getMap.set(pathname, handler);
  }

  post(pathname: string, handler: $OnFetch) {
    this._postMap.set(pathname, handler);
  }

  listen: $OnFetch = async (event: FetchEvent) => {
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
    console.error("error");
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
