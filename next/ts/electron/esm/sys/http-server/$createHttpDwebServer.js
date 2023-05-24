import { once } from "lodash";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.js";
import { IPC_ROLE } from "../../core/ipc/const.js";
import { buildUrl } from "../../helper/urlHelper.js";
import { ServerStartResult, ServerUrlInfo } from "./const.js";
/** 创建一个网络服务 */
export const createHttpDwebServer = async (microModule, options) => {
    /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
    const startResult = await startHttpDwebServer(microModule, options);
    console.log("获得域名授权：", startResult.urlInfo.internal_origin, startResult.urlInfo.public_origin);
    return new HttpDwebServer(microModule, options, startResult);
};
export class HttpDwebServer {
    constructor(nmm, options, startResult) {
        Object.defineProperty(this, "nmm", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: nmm
        });
        Object.defineProperty(this, "options", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: options
        });
        Object.defineProperty(this, "startResult", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: startResult
        });
        /** 开始处理请求 */
        Object.defineProperty(this, "listen", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (routes = [
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
            ]) => {
                return listenHttpDwebServer(this.nmm, this.startResult, routes);
            }
        });
        /** 关闭监听 */
        Object.defineProperty(this, "close", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: once(() => closeHttpDwebServer(this.nmm, this.options))
        });
    }
}
/** 开始处理请求 */
export const listenHttpDwebServer = async (microModule, startResult, routes = [
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
]) => {
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
    // console.log('[$createHttpDwebServer.cts 调用 microModule.nativeFetch],int', int)
    const httpIncomeRequestStream = await microModule
        .nativeFetch(buildUrlValue, int)
        .stream();
    console.log("开始响应服务请求");
    httpServerIpc.bindIncomeStream(httpIncomeRequestStream);
    return httpServerIpc;
};
/** 开始监听端口和域名 */
export const startHttpDwebServer = (microModule, options) => {
    const url = buildUrl(new URL(`file://http.sys.dweb/start`), {
        search: options,
    });
    return microModule
        .nativeFetch(url)
        .object()
        .then((obj) => {
        const { urlInfo, token } = obj;
        const serverUrlInfo = new ServerUrlInfo(urlInfo.host, urlInfo.internal_origin, urlInfo.public_origin);
        return new ServerStartResult(token, serverUrlInfo);
    });
};
/** 停止监听端口和域名 */
export const closeHttpDwebServer = async (microModule, options) => {
    return microModule
        .nativeFetch(buildUrl(new URL(`file://http.sys.dweb/close`), {
        search: options,
    }))
        .boolean();
};
