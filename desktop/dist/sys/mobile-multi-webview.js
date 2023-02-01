"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MobileMultiWebviewNMM = void 0;
const micro_module_native_js_1 = require("../core/micro-module.native.js");
/**
 * 构建一个视图树
 * 如果是桌面版，所以不用去管树的概念，直接生成生成就行了
 * 但这里是模拟手机版，所以还是构建一个层级视图
 */
class MobileMultiWebviewNMM extends micro_module_native_js_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = "mwebview.sys.dweb";
    }
    bootstrap() {
        nw.Window.open("../../multi-webview.html", {
            id: "multi-webview",
            show_in_taskbar: true,
        });
    }
    destroy() { }
}
exports.MobileMultiWebviewNMM = MobileMultiWebviewNMM;
