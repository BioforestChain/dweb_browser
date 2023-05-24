"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DownloadNMM = void 0;
const micro_module_native_js_1 = require("../../core/micro-module.native.js");
const devtools_js_1 = require("../../helper/devtools.js");
const www_server_js_1 = require("./www-server.js");
// 提供下载的 UI 
class DownloadNMM extends micro_module_native_js_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "download.sys.dweb"
        });
        Object.defineProperty(this, "httpNMM", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "waitForOperationResMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
    }
    async _bootstrap(context) {
        devtools_js_1.log.green(`[${this.mmid}] _bootstrap`);
        this.httpNMM = (await context.dns.query('http.sys.dweb'));
        if (this.httpNMM === undefined)
            throw new Error(`[${this.mmid}] this.httpNMM === undefined`);
        {
            new www_server_js_1.WWWServer(this);
        }
    }
    _shutdown() {
        throw new Error("Method not implemented.");
    }
}
exports.DownloadNMM = DownloadNMM;
