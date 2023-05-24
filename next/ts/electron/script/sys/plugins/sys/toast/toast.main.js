"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ToastNMM = void 0;
const micro_module_native_js_1 = require("../../../../core/micro-module.native.js");
const devtools_js_1 = require("../../../../helper/devtools.js");
const handlers_js_1 = require("./handlers.js");
class ToastNMM extends micro_module_native_js_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        // 
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "toast.sys.dweb"
        });
        Object.defineProperty(this, "_bootstrap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (context) => {
                devtools_js_1.log.green(`[${this.mmid} _bootstrap]`);
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/show",
                    matchMode: "full",
                    input: {},
                    output: "object",
                    handler: handlers_js_1.show.bind(this)
                });
            }
        });
        Object.defineProperty(this, "_shutdown", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async () => {
            }
        });
    }
}
exports.ToastNMM = ToastNMM;
