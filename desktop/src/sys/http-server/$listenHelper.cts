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
  const { origin, token } = await listenHttpDwebServer(microModule, options);
  console.log("获得域名授权：", origin, token);

  /** 开始才处理请求 */
  const start = () => handleHttpDwebServer(microModule, token);

  /** 关闭监听 */
  const close = once(() => unlistenHttpDwebServer(microModule, options));

  return { origin, token, start, close };
};

/** 开始处理请求 */
export const handleHttpDwebServer = async (
  microModule: $MicroModule,
  token: string
) => {
  /// 创建一个基于 二进制流的 ipc 信道
  const httpServerIpc = new ReadableStreamIpc(
    microModule,
    IPC_ROLE.CLIENT,
    true
  );
  const httpIncomeRequestStream = await microModule
    .fetch(
      buildUrl(new URL(`file://http.sys.dweb`), {
        pathname: "/on-request",
        search: {
          token,
          routes: [
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
          ] satisfies $ReqMatcher[],
        },
      }),
      {
        method: "POST",
        /// 这是上行的通道
        body: httpServerIpc.stream,
      }
    )
    .stream();

  console.log("开始响应服务请求");

  httpServerIpc.bindIncomeStream(httpIncomeRequestStream);
  return httpServerIpc;
};

/** 开始监听端口和域名 */
export const listenHttpDwebServer = (
  microModule: $MicroModule,
  options: Omit<$GetHostOptions, "ipc">
) => {
  return microModule
    .fetch(
      buildUrl(new URL(`file://http.sys.dweb/listen`), {
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
export const unlistenHttpDwebServer = async (
  microModule: $MicroModule,
  options: Omit<$GetHostOptions, "ipc">
) => {
  return microModule
    .fetch(
      buildUrl(new URL(`file://http.sys.dweb/unlisten`), {
        search: options,
      })
    )
    .boolean();
};
