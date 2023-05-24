import crypto from "crypto";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.js";
import { IPC_ROLE } from "../../core/ipc/const.js";
import { NativeMicroModule } from "../../core/micro-module.native.js";
import { ServerStartResult, ServerUrlInfo } from "./const.js";
import { defaultErrorResponse } from "./defaultErrorResponse.js";
import { Http1Server } from "./net/Http1Server.js";
import { PortListener } from "./portListener.js";
import { log } from "../../helper/devtools.js";
/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export class HttpServerNMM extends NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: `http.sys.dweb`
        });
        Object.defineProperty(this, "_dwebServer", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Http1Server()
        });
        Object.defineProperty(this, "_tokenMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "_gatewayMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "_info", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_allRoutes", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        // 获取 host
        Object.defineProperty(this, "getHostByReq", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (req) => {
                /// 获取 host
                var header_host = null;
                var header_x_dweb_host = null;
                var header_user_agent_host = null;
                var query_x_web_host = new URL(req.url || "/", this._dwebServer.origin).searchParams.get("X-Dweb-Host");
                for (const [key, value] of Object.entries(req.headers)) {
                    switch (key) {
                        case "host":
                        case "Host": {
                            if (typeof value === "string") {
                                header_host = value;
                                /// 桌面模式下，我们没有对链接进行拦截，将其转化为 `public_origin?X-Dweb-Host` 这种链接形式 ，因为支持 *.localhost 通配符这种域名
                                /// 所以这里只需要将 host 中的信息提取出来
                                if (value.endsWith(`.${this._dwebServer.authority}`)) {
                                    query_x_web_host = value
                                        .slice(0, -this._dwebServer.authority.length - 1)
                                        .replace(/-(\d+)/, ":$1");
                                }
                            }
                            break;
                        }
                        case "x-dweb-host":
                        case "X-Dweb-Host": {
                            if (typeof value === "string") {
                                header_x_dweb_host = value;
                            }
                        }
                        case "user-agent":
                        case "User-Agent": {
                            if (typeof value === "string") {
                                const host = value.match(/\sdweb-host\/(.+)\s*/)?.[1];
                                if (typeof host === "string") {
                                    header_user_agent_host = host;
                                }
                            }
                        }
                    }
                }
                let host = query_x_web_host ||
                    header_x_dweb_host ||
                    header_user_agent_host ||
                    header_host;
                if (typeof host === "string" && host.includes(":") === false) {
                    host += ":" + this._info?.protocol.port;
                }
                if (typeof host !== "string") {
                    /** 如果有需要，可以内部实现这个 key 为 "*" 的 listener 来提供默认服务 */
                    host = "*";
                }
                return host;
            }
        });
        // 获取 host
        Object.defineProperty(this, "getHostByURLAndHeaders", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (url, headers) => {
                /// 获取 host
                var header_host = null;
                var header_x_dweb_host = null;
                var header_user_agent_host = null;
                var query_x_web_host = new URL(url || "/", this._dwebServer.origin).searchParams.get("X-Dweb-Host");
                for (const [key, value] of Object.entries(headers)) {
                    switch (key) {
                        case "host":
                        case "Host": {
                            if (typeof value === "string") {
                                header_host = value;
                                /// 桌面模式下，我们没有对链接进行拦截，将其转化为 `public_origin?X-Dweb-Host` 这种链接形式 ，因为支持 *.localhost 通配符这种域名
                                /// 所以这里只需要将 host 中的信息提取出来
                                if (value.endsWith(`.${this._dwebServer.authority}`)) {
                                    query_x_web_host = value
                                        .slice(0, -this._dwebServer.authority.length - 1)
                                        .replace(/-(\d+)/, ":$1");
                                }
                            }
                            break;
                        }
                        case "x-dweb-host":
                        case "X-Dweb-Host": {
                            if (typeof value === "string") {
                                header_x_dweb_host = value;
                            }
                        }
                        case "user-agent":
                        case "User-Agent": {
                            if (typeof value === "string") {
                                const host = value.match(/\sdweb-host\/(.+)\s*/)?.[1];
                                if (typeof host === "string") {
                                    header_user_agent_host = host;
                                }
                            }
                        }
                    }
                }
                let host = query_x_web_host ||
                    header_x_dweb_host ||
                    header_user_agent_host ||
                    header_host;
                if (typeof host === "string" && host.includes(":") === false) {
                    host += ":" + this._info?.protocol.port;
                }
                if (typeof host !== "string") {
                    /** 如果有需要，可以内部实现这个 key 为 "*" 的 listener 来提供默认服务 */
                    host = "*";
                }
                return host;
            }
        });
    }
    async _bootstrap() {
        log.green(`${this.mmid} _bootstrap`);
        // 用来接受 推送的消息
        this.onConnect((remoteIpc) => {
            remoteIpc.onEvent((ipcEventMessage, ipc) => {
            });
        });
        // 创建了一个基础的 http 服务器 所有的 http:// 请求会全部会发送到这个地方来处理
        this._info = await this._dwebServer.create();
        console.log('创建了服务： ', this._info);
        this._info.server.on("request", async (req, res) => {
            console.log('接收到熬了请求 --- ');
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Headers", "*");
            res.setHeader("Access-Control-Allow-Methods", "*");
            // 根据发送改过来的请求 创建一个新的request
            // console.log('req.url: ', req.url)
            // console.log('headers: ', req.headers)
            // const _url = req.url?.split("request_path_100=")[1]
            // if(_url === undefined) throw new Error(`_url === undefined`);
            // const urlObj = new URL(_url)
            // const targetUrl = _url.replace(urlObj.origin, "")
            // // 需要更新request 试试采用 express 是否可以实现？？
            // console.log("targetUrl: ", targetUrl)
            // /// 获取 host
            // const host = this.getHostByURLAndHeaders(targetUrl, req.headers)
            // console.log('host: ', host)
            const host = this.getHostByReq(req);
            {
                // 在网关中寻址能够处理该 host 的监听者
                const gateway = this._gatewayMap.get(host);
                if (gateway == undefined) {
                    log.red(`[http-server onRequest 既没分发也没有匹配 gatewaty请求] ${req.url}`);
                    return defaultErrorResponse(req, res, 502, "Bad Gateway", "作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应");
                }
                // gateway.listener.ipc.request("/on-connect")
                // const gateway_timeout = setTimeout(() => {
                //   if (res.writableLength === 0) {
                //   }
                //   res.write;
                //   res.hasHeader;
                // }, 3e4 /* 30s 没有任何 body 写入的话，认为网关超时 */);
                // 源代码
                void gateway.listener.hookHttpRequest(req, res);
            }
        });
        /// 监听 IPC 请求 /start
        this.registerCommonIpcOnMessageHandler({
            pathname: "/start",
            matchMode: "full",
            input: { port: "number?", subdomain: "string?" },
            output: "object",
            handler: async (args, ipc) => {
                return await this.start(ipc, args);
            },
        });
        /// 监听 IPC 请求 /close
        this.registerCommonIpcOnMessageHandler({
            pathname: "/close",
            matchMode: "full",
            input: { port: "number?", subdomain: "string?" },
            output: "boolean",
            handler: async (args, ipc) => {
                return await this.close(ipc, args);
            },
        });
        /// 监听 IPC 请求 /listen post
        this.registerCommonIpcOnMessageHandler({
            method: "POST",
            pathname: "/listen",
            matchMode: "full",
            input: { token: "string", routes: "object" },
            output: "object",
            handler: async (args, ipc, message) => {
                return this.listen(args.token, message, args.routes);
            },
        });
    }
    _shutdown() {
        this._dwebServer.destroy();
    }
    getServerUrlInfo(ipc, options) {
        const mmid = ipc.remote.mmid;
        const { subdomain: options_subdomain = "", port = 80 } = options;
        const subdomainPrefix = options_subdomain === "" || options_subdomain.endsWith(".")
            ? options_subdomain
            : `${options_subdomain}.`;
        if (port <= 0 || port >= 65536) {
            throw new Error(`invalid dweb http port: ${port}`);
        }
        const public_origin = this._dwebServer.origin;
        const host = `${subdomainPrefix}${mmid}:${port}`;
        const internal_origin = `http://${subdomainPrefix}${mmid}-${port}.${this._dwebServer.authority}`;
        return new ServerUrlInfo(host, internal_origin, public_origin);
    }
    /** 申请监听，获得一个连接地址 */
    async start(ipc, hostOptions) {
        const serverUrlInfo = this.getServerUrlInfo(ipc, hostOptions);
        if (this._gatewayMap.has(serverUrlInfo.host)) {
            throw new Error(`already in listen: ${serverUrlInfo.internal_origin}`);
        }
        const listener = new PortListener(ipc, serverUrlInfo.host, serverUrlInfo.internal_origin);
        /// ipc 在关闭的时候，自动释放所有的绑定
        listener.onDestroy(ipc.onClose(() => {
            this.close(ipc, hostOptions);
        }));
        // jmmMetadata.sys.dweb-80.localhost:22605
        // jmmmetadata.sys.dweb-80.localhost:22605
        const token = Buffer.from(crypto.getRandomValues(new Uint8Array(64))).toString("base64url");
        const gateway = { listener, urlInfo: serverUrlInfo, token };
        this._tokenMap.set(token, gateway);
        this._gatewayMap.set(serverUrlInfo.host, gateway);
        return new ServerStartResult(token, serverUrlInfo);
    }
    /** 远端监听请求，将提供一个 ReadableStreamIpc 流 */
    async listen(token, message, routes) {
        const gateway = this._tokenMap.get(token);
        if (gateway === undefined) {
            throw new Error(`no gateway with token: ${token}`);
        }
        const streamIpc = new ReadableStreamIpc(gateway.listener.ipc.remote, IPC_ROLE.CLIENT);
        void streamIpc.bindIncomeStream(message.body.stream());
        streamIpc.onClose(gateway.listener.addRouter({
            routes,
            streamIpc,
        }));
        return new Response(streamIpc.stream, { status: 200 });
    }
    /**
     * 释放监听
     */
    close(ipc, hostOptions) {
        const serverUrlInfo = this.getServerUrlInfo(ipc, hostOptions);
        const gateway = this._gatewayMap.get(serverUrlInfo.host);
        if (gateway === undefined) {
            return false;
        }
        this._tokenMap.delete(gateway.token);
        this._gatewayMap.delete(serverUrlInfo.host);
        /// 执行销毁
        gateway.listener.destroy();
        return true;
    }
}
