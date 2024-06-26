import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { buildUrl } from "@dweb-browser/helper/fun/urlHelper.ts";
import { logger } from "@dweb-browser/helper/logger.ts";
import type { MicroModuleRuntime } from "../MicroModule.ts";
import { createFetchHandler, type $OnFetch } from "../ipc/helper/ipcFetchHelper.ts";
import { IpcResponse } from "../ipc/index.ts";
import type { Ipc } from "../ipc/ipc.ts";
import type { $ReqMatcher } from "../type/$ReqMatcher.ts";
import { ServerStartResult, ServerUrlInfo } from "./const.ts";
import type { $DwebHttpServerOptions } from "./types.ts";

/** 创建一个网络服务 */
export const createHttpDwebServer = async (
  microModule: MicroModuleRuntime,
  options: $DwebHttpServerOptions,
  target: string
) => {
  /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
  const startResult = await startHttpDwebServer(microModule, options);
  console.log(
    "http/dweb-server",
    "获得域名授权：",
    startResult.urlInfo.internal_origin,
    startResult.urlInfo.public_origin
  );
  return new HttpDwebServer(microModule, options, startResult, target);
};

export class HttpDwebServer {
  readonly console;
  constructor(
    private readonly nmm: MicroModuleRuntime,
    private readonly options: $DwebHttpServerOptions,
    readonly startResult: ServerStartResult,
    readonly target: string
  ) {
    this.console = logger(`http<${options.subdomain ? `${options.subdomain}.` : ""}${nmm.mmid}>`);
  }
  /** 开始处理请求 */
  listen = $once(async (...onFetchs: $OnFetch[]) => {
    const serverIpc = await listenHttpDwebServer(this.nmm, this.startResult, this.target);

    const fetchHandler = createFetchHandler(onFetchs);
    serverIpc.onRequest(this.target).collect(async (event) => {
      const request = event.consume();
      this.console.verbose("http-in", request);
      const response =
        (await fetchHandler(request)) || IpcResponse.fromText(request.reqId, 404, undefined, "", serverIpc);
      serverIpc.postMessage(response);
    });
    return Object.assign(fetchHandler, { ipc: serverIpc });
  });
  /** 关闭监听 */
  close = $once(async () => {
    if (this.listen.hasRun) {
      const { ipc } = await this.listen.result;
      ipc.close();
    }
    await closeHttpDwebServer(this.nmm, this.options);
  });
}
/** 开始监听端口和域名 */
const startHttpDwebServer = async (microModule: MicroModuleRuntime, options: $DwebHttpServerOptions) => {
  const url = buildUrl(new URL(`file://http.std.dweb/start`), {
    search: options,
  });
  return await microModule
    .nativeFetch(url)
    .object<ServerStartResult>()
    .then((obj) => {
      const { urlInfo, token } = obj;
      const serverUrlInfo = new ServerUrlInfo(urlInfo.host, urlInfo.internal_origin, urlInfo.public_origin);
      return new ServerStartResult(token, serverUrlInfo);
    })
    .catch((e) => {
      console.log("startHttpDwebServer error=>", e);
      throw e;
    });
};

/** 开始处理请求 */
const listenHttpDwebServer = async (
  microModule: MicroModuleRuntime,
  startResult: ServerStartResult,
  target: string,
  routes: $ReqMatcher[] = [
    /** 定义了路由的方法 */
    { pathname: "/", matchMode: "prefix", method: "GET,POST,PUT,DELETE,PATCH,OPTIONS,HEAD,CONNECT,TRACE" },
  ] satisfies $ReqMatcher[],
  customServerIpc?: Ipc
) => {
  const serverIpc =
    customServerIpc ??
    (await (async () => {
      const httpIpc = await microModule.connect("http.std.dweb");
      return await httpIpc.fork(undefined, undefined, true, `listenHttpDwebServer-${target}`);
    })());
  microModule.console.log("listenHttpDwebServer", serverIpc);
  await serverIpc.request(
    buildUrl(`file://http.std.dweb/listen`, {
      search: {
        token: startResult.token,
        routes,
      },
    }).href
  );

  return serverIpc;
};

/** 停止监听端口和域名 */
const closeHttpDwebServer = async (microModule: MicroModuleRuntime, options: $DwebHttpServerOptions) => {
  return await microModule
    .nativeFetch(
      buildUrl(new URL(`file://http.std.dweb/close`), {
        search: options,
      })
    )
    .boolean();
};
