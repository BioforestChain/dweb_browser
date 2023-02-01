"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MultiWebviewNMM = void 0;
const helper_cjs_1 = require("../core/helper.cjs");
const micro_module_native_cjs_1 = require("../core/micro-module.native.cjs");
/**
 * 构建一个视图树
 * 如果是桌面版，所以不用去管树的概念，直接生成生成就行了
 * 但这里是模拟手机版，所以还是构建一个层级视图
 */
class MultiWebviewNMM extends micro_module_native_cjs_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = "mwebview.sys.dweb";
    }
    async _bootstrap() {
        const window = await (0, helper_cjs_1.openNwWindow)("../../multi-webview.html", {
            id: "multi-webview",
            show_in_taskbar: true,
        });
        if (window.window.APIS_READY !== true) {
            await new Promise((resolve) => {
                window.window.addEventListener("apis-ready", resolve);
            });
        }
        const APIS = window.window;
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/open",
            matchMode: "full",
            input: { url: "string" },
            output: "number",
            hanlder: (args, ipc) => {
                return APIS.openWebview(ipc.uid, args.url);
            },
        });
        this.window = window;
    }
    _shutdown() {
        this.window?.close();
        this.window = undefined;
    }
}
exports.MultiWebviewNMM = MultiWebviewNMM;
