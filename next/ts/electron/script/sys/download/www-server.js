"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.WWWServer = void 0;
const devtools_js_1 = require("../../helper/devtools.js");
const base_www_server_js_1 = require("../plugins/base/base_www_server.js");
class WWWServer extends base_www_server_js_1.BaseWWWServer {
    constructor(nmm) {
        super(nmm);
        Object.defineProperty(this, "_onRequestMore", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (message, ipc) => {
                devtools_js_1.log.red(`${this.nmm.mmid} www-server.cts.cts _onMessage 还有消息没有处理 ${JSON.stringify(message)}`);
            }
        });
    }
}
exports.WWWServer = WWWServer;
