"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BaseWWWServer = void 0;
const devtools_js_1 = require("../../../helper/devtools.js");
const const_js_1 = require("../../../core/ipc/const.js");
const _createHttpDwebServer_js_1 = require("../../http-server/$createHttpDwebServer.js");
const IpcResponse_js_1 = require("../../../core/ipc/IpcResponse.js");
/**
 * 提供静态资源服务
 */
class BaseWWWServer {
    constructor(nmm) {
        Object.defineProperty(this, "nmm", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: nmm
        });
        Object.defineProperty(this, "server", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_int", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async () => {
                this.server = await (0, _createHttpDwebServer_js_1.createHttpDwebServer)(this.nmm, {});
                devtools_js_1.log.green(`[${this.nmm.mmid}] ${this.server.startResult.urlInfo.internal_origin}`);
                (await this.server.listen()).onMessage(this._onMessage);
            }
        });
        Object.defineProperty(this, "_onMessage", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (message, ipc) => {
                switch (message.type) {
                    case const_js_1.IPC_MESSAGE_TYPE.REQUEST:
                        this._onRequest(message, ipc);
                        break;
                    default: this._onRequestMore(message, ipc);
                    // default: 
                }
            }
        });
        Object.defineProperty(this, "_onRequest", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (request, ipc) => {
                let pathname = request.parsed_url.pathname;
                if (pathname === "/" || pathname === "/index.html") {
                    pathname = `/html/${this.nmm.mmid}.html`;
                }
                const url = `file:///assets/${pathname}?mode=stream`;
                const remoteIpcResponse = await this.nmm.nativeFetch(url);
                ipc.postMessage(await IpcResponse_js_1.IpcResponse.fromResponse(request.req_id, remoteIpcResponse, ipc));
            }
        });
        this._int();
    }
}
exports.BaseWWWServer = BaseWWWServer;
