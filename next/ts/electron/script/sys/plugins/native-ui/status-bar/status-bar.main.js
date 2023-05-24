"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.$StatusbarStyle = exports.StatusbarNativeUiNMM = void 0;
// 模拟状态栏模块-用来提供状态UI的模块
const micro_module_native_js_1 = require("../../../../core/micro-module.native.js");
const devtools_js_1 = require("../../../../helper/devtools.js");
const handlers_js_1 = require("./handlers.js");
const IpcEvent_js_1 = require("../../../../core/ipc/IpcEvent.js");
const electron_1 = require("electron");
// import type { $Schema1, $Schema2 } from "../../../../helper/types.ts";
class StatusbarNativeUiNMM extends micro_module_native_js_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "status-bar.nativeui.sys.dweb"
        });
        Object.defineProperty(this, "httpIpc", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "observes", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "observesState", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "encoder", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new TextEncoder()
        });
        Object.defineProperty(this, "_bootstrap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (context) => {
                devtools_js_1.log.green(`[${this.mmid} _bootstrap]`);
                {
                    // 监听从 multi-webview-comp-status-bar.html.mts 通过 ipcRenderer 发送过来的 监听数据
                    electron_1.ipcMain.on('status_bar_state_change', (ipcMainEvent, host, statusbarState) => {
                        const b = this.observesState.get(host);
                        if (b === true) {
                            const ipc = this.observes.get(host);
                            if (ipc === undefined)
                                throw new Error(`ipc === undefined`);
                            ipc.postMessage(IpcEvent_js_1.IpcEvent.fromText("observe", `${JSON.stringify(statusbarState)}`));
                        }
                    });
                }
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/getState",
                    matchMode: "full",
                    input: {},
                    output: "object",
                    handler: handlers_js_1.getState.bind(this)
                });
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/setState",
                    matchMode: "full",
                    input: {},
                    output: "object",
                    handler: handlers_js_1.setState.bind(this)
                });
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/startObserve",
                    matchMode: "full",
                    input: {},
                    output: "boolean",
                    handler: handlers_js_1.startObserve.bind(this)
                });
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/stopObserve",
                    matchMode: "full",
                    input: {},
                    output: "boolean",
                    handler: handlers_js_1.stopObserve.bind(this)
                });
            }
        });
    }
    _onConnect(ipc) {
        ipc.onEvent((event) => {
            if (event.name === "observe") {
                const host = event.data;
                this.observes.set(host, ipc);
            }
            if (event.name === "updated") {
            }
        });
    }
    _shutdown() {
    }
}
exports.StatusbarNativeUiNMM = StatusbarNativeUiNMM;
var $StatusbarStyle;
(function ($StatusbarStyle) {
    $StatusbarStyle["light"] = "light";
    $StatusbarStyle["dark"] = "dark";
    $StatusbarStyle["default"] = "default";
})($StatusbarStyle = exports.$StatusbarStyle || (exports.$StatusbarStyle = {}));
