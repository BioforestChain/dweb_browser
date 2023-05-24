"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.closeHttpDwebServer = exports.startHttpDwebServer = exports.listenHttpDwebServer = exports.HttpDwebServer = exports.createHttpDwebServer = void 0;
const lodash_1 = require("lodash");
const ReadableStreamIpc_js_1 = require("../../core/ipc-web/ReadableStreamIpc.js");
const const_js_1 = require("../../core/ipc/const.js");
const urlHelper_js_1 = require("../../helper/urlHelper.js");
const const_js_2 = require("./const.js");
/** 创建一个网络服务 */
const createHttpDwebServer = async (microModule, options) => {
    /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
    const startResult = await (0, exports.startHttpDwebServer)(microModule, options);
    console.log("获得域名授权：", startResult.urlInfo.internal_origin, startResult.urlInfo.public_origin);
    return new HttpDwebServer(microModule, options, startResult);
};
exports.createHttpDwebServer = createHttpDwebServer;
class HttpDwebServer {
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
                return (0, exports.listenHttpDwebServer)(this.nmm, this.startResult, routes);
            }
        });
        /** 关闭监听 */
        Object.defineProperty(this, "close", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, lodash_1.once)(() => (0, exports.closeHttpDwebServer)(this.nmm, this.options))
        });
    }
}
exports.HttpDwebServer = HttpDwebServer;
/** 开始处理请求 */
const listenHttpDwebServer = async (microModule, startResult, routes = [
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
    const httpServerIpc = new ReadableStreamIpc_js_1.ReadableStreamIpc(microModule, const_js_1.IPC_ROLE.CLIENT);
    const url = new URL(`file://http.sys.dweb`);
    const ext = {
        pathname: "/listen",
        search: {
            host: startResult.urlInfo.host,
            token: startResult.token,
            routes,
        },
    };
    const buildUrlValue = (0, urlHelper_js_1.buildUrl)(url, ext);
    const int = { method: "POST", body: httpServerIpc.stream };
    // console.log('[$createHttpDwebServer.cts 调用 microModule.nativeFetch],int', int)
    const httpIncomeRequestStream = await microModule
        .nativeFetch(buildUrlValue, int)
        .stream();
    console.log("开始响应服务请求");
    httpServerIpc.bindIncomeStream(httpIncomeRequestStream);
    return httpServerIpc;
};
exports.listenHttpDwebServer = listenHttpDwebServer;
/** 开始监听端口和域名 */
const startHttpDwebServer = (microModule, options) => {
    const url = (0, urlHelper_js_1.buildUrl)(new URL(`file://http.sys.dweb/start`), {
        search: options,
    });
    return microModule
        .nativeFetch(url)
        .object()
        .then((obj) => {
        const { urlInfo, token } = obj;
        const serverUrlInfo = new const_js_2.ServerUrlInfo(urlInfo.host, urlInfo.internal_origin, urlInfo.public_origin);
        return new const_js_2.ServerStartResult(token, serverUrlInfo);
    });
};
exports.startHttpDwebServer = startHttpDwebServer;
/** 停止监听端口和域名 */
const closeHttpDwebServer = async (microModule, options) => {
    return microModule
        .nativeFetch((0, urlHelper_js_1.buildUrl)(new URL(`file://http.sys.dweb/close`), {
        search: options,
    }))
        .boolean();
};
exports.closeHttpDwebServer = closeHttpDwebServer;
