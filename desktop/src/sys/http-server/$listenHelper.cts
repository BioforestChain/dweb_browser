import once from "lodash/once";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.cjs";
import { IPC_ROLE } from "../../core/ipc/const.cjs";
import type { $ReqMatcher } from "../../helper/$ReqMatcher.cjs";
import type { $MicroModule } from "../../helper/types.cjs";
import { buildUrl } from "../../helper/urlHelper.cjs";
import type { $GetHostOptions } from "./net/createNetServer.cjs";

/** 创建一个网络服务 */
export const createHttpDwebServer = async (
  microModule: $MicroModule,
  options: Omit<$GetHostOptions, "ipc">
) => {
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
  const { origin, token } = await startHttpDwebServer(microModule, options);
  console.log("获得域名授权：", origin, token);

  /** 开始才处理请求 */
  const listen = () => listenHttpDwebServer(microModule, token);

  /** 关闭监听 */
  const close = once(() => closeHttpDwebServer(microModule, options));

  return { origin, token, listen: listen, close };
};

/** 开始处理请求 */
export const listenHttpDwebServer = async (
  microModule: $MicroModule,
  token: string
) => {
  /// 创建一个基于 二进制流的 ipc 信道
  const httpServerIpc = new ReadableStreamIpc(microModule,IPC_ROLE.CLIENT,true);
  const routes = [ /** 定义了路由的方法 */
    { pathname: "/", matchMode: "prefix", method: "GET", },
    { pathname: "/", matchMode: "prefix", method: "POST", },
    { pathname: "/", matchMode: 'prefix',  method:'PUT'},
    { pathname: "/", matchMode: 'prefix', method: "DELETE"},
    { pathname: "/", matchMode: 'prefix', method: "PATCH"},
    { pathname: "/", matchMode: 'prefix', method: "OPTIONS"},
    { pathname: "/", matchMode: 'prefix', method: "HEAD"},
    { pathname: "/", matchMode: 'prefix', method: "CONNECT"},
    { pathname: "/", matchMode: 'prefix', method: "TRACE"},
  ] satisfies $ReqMatcher[]
  const url = new URL(`file://http.sys.dweb`);
  const ext = {
    pathname: "/listen",
    search: {
      token,
      routes,
    },
  }
  const buildUrlValue = buildUrl(url, ext);
  const int = {method: "POST", body: httpServerIpc.stream}
  const httpIncomeRequestStream = await microModule.fetch(buildUrlValue, int).stream()

  // const httpIncomeRequestStream = await microModule
  // .fetch(
  //     buildUrl(
  //       new URL(`file://http.sys.dweb`), 
  //       {
  //         pathname: "/listen",
  //         search: {
  //           token,
  //           routes: [ /** 定义了路由的方法 */
  //             { pathname: "/", matchMode: "prefix", method: "GET", },
  //             { pathname: "/", matchMode: "prefix", method: "POST", },
  //             { pathname: "/", matchMode: 'prefix',  method:'PUT'},
  //             { pathname: "/", matchMode: 'prefix', method: "DELETE"},
  //             { pathname: "/", matchMode: 'prefix', method: "PATCH"},
  //             { pathname: "/", matchMode: 'prefix', method: "OPTIONS"},
  //             { pathname: "/", matchMode: 'prefix', method: "HEAD"},
  //             { pathname: "/", matchMode: 'prefix', method: "CONNECT"},
  //             { pathname: "/", matchMode: 'prefix', method: "TRACE"},
  //           ] satisfies $ReqMatcher[],
  //         },
  //       }
  //     ),
  //     {
  //       method: "POST",
  //       /// 这是上行的通道
  //       body: httpServerIpc.stream,
  //     }
  //   )
  //   .stream();

  console.log("开始响应服务请求");

  httpServerIpc.bindIncomeStream(httpIncomeRequestStream);
  return httpServerIpc;
};

/** 开始监听端口和域名 */
export const startHttpDwebServer = (
  microModule: $MicroModule,
  options: Omit<$GetHostOptions, "ipc">
) => {
  return microModule
    .fetch(
      buildUrl(new URL(`file://http.sys.dweb/start`), {
        search: options,
      })
    )
    .object<{
      origin: string;
      token: string;
    }>()
    .then((obj) => {
      console.log(obj);
      return obj;
    });
};

/** 停止监听端口和域名 */
export const closeHttpDwebServer = async (
  microModule: $MicroModule,
  options: Omit<$GetHostOptions, "ipc">
) => {
  return microModule
    .fetch(
      buildUrl(new URL(`file://http.sys.dweb/close`), {
        search: options,
      })
    )
    .boolean();
};
