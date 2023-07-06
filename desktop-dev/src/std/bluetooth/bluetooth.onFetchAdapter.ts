import type { $OnFetch, FetchEvent } from "../../core/helper/ipcFetchHelper.ts";
import { IPC_METHOD } from "../../core/ipc/const.ts";
import { IpcHeaders, IpcResponse } from "../../core/ipc/index.ts";

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

// 为了加强语义化进行的修改
// - 把类型的名称从router 改为 onFetchAdapter
// - 把 get 和 post 方法 改为使用 add
// - 把 listen 改为 run

// 实现上的修改
// mathc 返回的是 实例自身，方便链式调用；

// 封装的考虑思路是
// 把代码分为两个层次
// 1. 业务层 - 尽量一眼就能够看明白当前模块提供的
//    1-1. _bootstrap方法中的 我希望能够告诉 旁观者我这个模块启动状态会执行的业务
//         createHttpDwebServer() 创建服务
//         addAdapter() 添加适配器
//         startListen() 开始监听请求
// 2. 具体执行层
//    2-1. 每一个执行层的函数都指包含一个独立的小功能

// 我对好的代码的理解
// 1. 让他人能够 非常容易的理解 我的这个模块干了什么
// 2. 我的模块能够 完成什么样的工作
