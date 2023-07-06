import type { $ReqMatcher } from "../../../core/helper/$ReqMatcher.ts";
import type { $MicroModule } from "../../../core/types.ts";
import type { $DwebHttpServerOptions } from "../net/createNetServer.ts";

import { ReadableStreamIpc } from "../../../core/ipc-web/ReadableStreamIpc.ts";
import { IPC_ROLE } from "../../../core/ipc/const.ts";

import { once } from "../../../helper/$once.ts";
import { PromiseOut } from "../../../helper/PromiseOut.ts";
import { buildUrl } from "../../../helper/urlHelper.ts";
import { ServerStartResult, ServerUrlInfo } from "../const.ts";

/** 创建一个网络服务 */
export const createHttpDwebServer = async (
  microModule: $MicroModule,
  options: $DwebHttpServerOptions
) => {
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
  const startResult = await startHttpDwebServer(microModule, options);
  console.log(
    "http/dweb-server",
    "获得域名授权：",
    startResult.urlInfo.internal_origin,
    startResult.urlInfo.public_origin
  );
  return new HttpDwebServer(microModule, options, startResult);
};

export class HttpDwebServer {
  constructor(
    private readonly nmm: $MicroModule,
    private readonly options: $DwebHttpServerOptions,
    readonly startResult: ServerStartResult
  ) {}
  private _listenIpcPo = new PromiseOut<ReadableStreamIpc>();
  private _listenIpcP?: Promise<ReadableStreamIpc>;
  /** 开始处理请求 */
  listen = (routes?: $ReqMatcher[]) => {
    this._listen(routes);
    return this._listenIpcPo.promise;
  };
  private _listen(routes?: $ReqMatcher[]) {
    if (this._listenIpcP) {
      throw new Error(
        "Listen method has been called more than once without closing."
      );
    }

    this._listenIpcPo.resolve(
      (this._listenIpcP = listenHttpDwebServer(
        this.nmm,
        this.startResult,
        routes
      ))
    );
  }
  /** 关闭监听 */
  close = once(async () => {
    if (this._listenIpcP) {
      const ipc = await this._listenIpcP;
      ipc.close();
      this._listenIpcP = undefined;
      this._listenIpcPo = PromiseOut.reject("dweb-http listen closed");
    }
    await closeHttpDwebServer(this.nmm, this.options);
  });
}
/** 开始处理请求 */
export const listenHttpDwebServer = async (
  microModule: $MicroModule,
  startResult: ServerStartResult,
  routes: $ReqMatcher[] = [
    /** 定义了路由的方法 */
    { pathname: "/", matchMode: "prefix", method: "GET" },
    { pathname: "/", matchMode: "prefix", method: "POST" },
    { pathname: "/", matchMode: "prefix", method: "PUT" },
    { pathname: "/", matchMode: "prefix", method: "DELETE" },
    { pathname: "/", matchMode: "prefix", method: "PATCH" },
    { pathname: "/", matchMode: "prefix", method: "OPTIONS" },
    { pathname: "/", matchMode: "prefix", method: "HEAD" },
    { pathname: "/", matchMode: "prefix", method: "CONNECT" },
    { pathname: "/", matchMode: "prefix", method: "TRACE" },
  ] satisfies $ReqMatcher[]
) => {
  /// 创建一个基于 二进制流的 ipc 信道
  const httpServerIpc = new ReadableStreamIpc(microModule, IPC_ROLE.CLIENT);
  const url = new URL(`file://http.std.dweb`);
  const ext = {
    pathname: "/listen",
    search: {
      host: startResult.urlInfo.host,
      token: startResult.token,
      routes,
    },
  };
  const buildUrlValue = buildUrl(url, ext);
  const int = { method: "POST", body: httpServerIpc.stream };
  const httpIncomeRequestStream = await microModule
    .nativeFetch(buildUrlValue, int)
    .stream();

  httpServerIpc.bindIncomeStream(httpIncomeRequestStream);
  /// 保存连接对象到池子中
  microModule.addToIpcSet(httpServerIpc);
  return httpServerIpc;
};

/** 开始监听端口和域名 */
export const startHttpDwebServer = async (
  microModule: $MicroModule,
  options: $DwebHttpServerOptions
) => {
  const url = buildUrl(new URL(`file://http.std.dweb/start`), {
    search: options,
  });
  return await microModule
    .nativeFetch(url)
    .object<ServerStartResult>()
    .then((obj) => {
      const { urlInfo, token } = obj;
      const serverUrlInfo = new ServerUrlInfo(
        urlInfo.host,
        urlInfo.internal_origin,
        urlInfo.public_origin
      );
      return new ServerStartResult(token, serverUrlInfo);
    });
};

/** 停止监听端口和域名 */
export const closeHttpDwebServer = async (
  microModule: $MicroModule,
  options: $DwebHttpServerOptions
) => {
  return await microModule
    .nativeFetch(
      buildUrl(new URL(`file://http.std.dweb/close`), {
        search: options,
      })
    )
    .boolean();
};
