import once from "lodash/once";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.cjs";
import { IPC_ROLE } from "../../core/ipc/const.cjs";
import type { $ReqMatcher } from "../../helper/$ReqMatcher.cjs";
import type { $MicroModule } from "../../helper/types.cjs";
import { buildUrl } from "../../helper/urlHelper.cjs";
import { ServerStartResult, ServerUrlInfo } from "./const.js";
import type { $DwebHttpServerOptions } from "./net/createNetServer.cjs";

/** 创建一个网络服务 */
export const createHttpDwebServer = async (
  microModule: $MicroModule,
  options: $DwebHttpServerOptions
) => {
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
  const startResult = await startHttpDwebServer(microModule, options);
  console.log("获得域名授权：", startResult);

  return new HttpDwebServer(microModule, options, startResult);
};

class HttpDwebServer {
  constructor(
    private readonly nmm: $MicroModule,
    private readonly options: $DwebHttpServerOptions,
    readonly startResult: ServerStartResult
  ) {}
  /** 开始处理请求 */
  async listen(
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
  ) {
    return listenHttpDwebServer(this.nmm, this.startResult.token, routes);
  }
  /** 关闭监听 */
  close = once(() => closeHttpDwebServer(this.nmm, this.options));
}

/** 开始处理请求 */
export const listenHttpDwebServer = async (
  microModule: $MicroModule,
  token: string,
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
      token,
      routes,
    },
  };
  const buildUrlValue = buildUrl(url, ext);
  const int = { method: "POST", body: httpServerIpc.stream };
  const httpIncomeRequestStream = await microModule
    .fetch(buildUrlValue, int)
    .stream();

  console.log("开始响应服务请求");

  httpServerIpc.bindIncomeStream(httpIncomeRequestStream);
  return httpServerIpc;
};

/** 开始监听端口和域名 */
export const startHttpDwebServer = (
  microModule: $MicroModule,
  options: $DwebHttpServerOptions
) => {
  return microModule
    .fetch(
      buildUrl(new URL(`file://http.sys.dweb/start`), {
        search: options,
      })
    )
    .object<ServerStartResult>()
    .then((obj) => {
      console.log(obj);
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
  return microModule
    .fetch(
      buildUrl(new URL(`file://http.sys.dweb/close`), {
        search: options,
      })
    )
    .boolean();
};
