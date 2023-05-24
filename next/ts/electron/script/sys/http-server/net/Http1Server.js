"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Http1Server = void 0;
const findPort_js_1 = require("../../../helper/findPort.js");
const createNetServer_js_1 = require("./createNetServer.js");
/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
class Http1Server extends createNetServer_js_1.NetServer {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "_info", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "bindingPort", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: -1
        });
        // _getHost(
        //   subdomain: string,
        //   mmid: string,
        //   port: number,
        //   info: $HttpServerInfo
        // ): string {
        //   return `${subdomain}${mmid}-${port}.localhost:${info.port}`;
        // }
    }
    get info() {
        return this._info;
    }
    get authority() {
        return `localhost:${this.bindingPort}`;
    }
    get origin() {
        return `${Http1Server.PREFIX}${this.authority}`;
    }
    async create() {
        /// 启动一个通用的网关服务
        const local_port = await (0, findPort_js_1.findPort)([(this.bindingPort = 22605)]);
        return (this._info = await (0, createNetServer_js_1.httpCreateServer)({}, {
            port: local_port,
        }));
    }
    destroy() {
        this._info?.server.close();
        this._info = undefined;
    }
}
Object.defineProperty(Http1Server, "PREFIX", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: "http://"
});
Object.defineProperty(Http1Server, "PROTOCOL", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: "http:"
});
Object.defineProperty(Http1Server, "PORT", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: 80
});
exports.Http1Server = Http1Server;
