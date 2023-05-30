import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.ts";
import { IPC_ROLE } from "../../core/ipc/const.ts";
import type { $ReqMatcher } from "../../helper/$ReqMatcher.ts";
import { once } from "../../helper/$once.ts";
import type { $MicroModule } from "../../helper/types.ts";
import { buildUrl } from "../../helper/urlHelper.ts";
import { ServerStartResult, ServerUrlInfo } from "./const.ts";
import type { $DwebHttpServerOptions } from "./net/createNetServer.ts";

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
  /** 开始处理请求 */
  listen = async (
    routes: $ReqMatcher[] = [
      {
        pathname: "/",
        matchMode: "prefix",
        method: "GET",
      },
      {
        pathname: "/",
        matchMode: "prefix",
        method: "POST",
      },
      {
        pathname: "/",
        matchMode: "prefix",
        method: "PUT",
      },
      {
        pathname: "/",
        matchMode: "prefix",
        method: "DELETE",
      },
    ]
  ) => {
    return await listenHttpDwebServer(this.nmm, this.startResult, routes);
  };
  /** 关闭监听 */
  close = once(() => closeHttpDwebServer(this.nmm, this.options));
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
  const url = new URL(`file://http.sys.dweb`);
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
  return httpServerIpc;
};

/** 开始监听端口和域名 */
export const startHttpDwebServer = (
  microModule: $MicroModule,
  options: $DwebHttpServerOptions
) => {
  const url = buildUrl(new URL(`file://http.sys.dweb/start`), {
    search: options,
  });
  return microModule
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
      buildUrl(new URL(`file://http.sys.dweb/close`), {
        search: options,
      })
    )
    .boolean();
};
