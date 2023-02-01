"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MultiWebviewNMM = void 0;
const micro_module_native_js_1 = require("../core/micro-module.native.js");
class MultiWebviewNMM extends micro_module_native_js_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = "desktop.sys.dweb";
    }
    bootstrap() {
        throw new Error("Method not implemented.");
    }
    destroy() {
        throw new Error("Method not implemented.");
    }
}
exports.MultiWebviewNMM = MultiWebviewNMM;
