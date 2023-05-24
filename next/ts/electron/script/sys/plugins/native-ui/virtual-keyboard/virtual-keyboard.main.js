"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.VirtualKeyboardNMM = void 0;
const micro_module_native_js_1 = require("../../../../core/micro-module.native.js");
const devtools_js_1 = require("../../../../helper/devtools.js");
const handlers_js_1 = require("./handlers.js");
const electron_1 = require("electron");
const index_js_1 = require("../../../../core/ipc/index.js");
class VirtualKeyboardNMM extends micro_module_native_js_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "virtual-keyboard.nativeui.sys.dweb"
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
                    electron_1.ipcMain.on("virtual_keyboard_state_change", (ipcMainEvent, host, statusbarState) => {
                        const b = this.observesState.get(host);
                        if (b === true) {
                            const ipc = this.observes.get(host);
                            if (ipc === undefined)
                                throw new Error(`ipc === undefined`);
                            ipc.postMessage(index_js_1.IpcEvent.fromText("observe", `${JSON.stringify(statusbarState)}`));
                        }
                    });
                }
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/getState",
                    matchMode: "full",
                    input: {},
                    output: "object",
                    handler: handlers_js_1.getState.bind(this),
                });
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/setState",
                    matchMode: "full",
                    input: {},
                    output: "object",
                    handler: handlers_js_1.setState.bind(this),
                });
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/startObserve",
                    matchMode: "full",
                    input: {},
                    output: "boolean",
                    handler: handlers_js_1.startObserve.bind(this),
                });
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/stopObserve",
                    matchMode: "full",
                    input: {},
                    output: "boolean",
                    handler: handlers_js_1.stopObserve.bind(this),
                });
            }
        });
        Object.defineProperty(this, "_shutdown", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async () => { }
        });
    }
    _onConnect(ipc) {
        ipc.onEvent((event) => {
            if (event.name === "observe") {
                const host = event.data;
                this.observes.set(host, ipc);
            }
        });
    }
}
exports.VirtualKeyboardNMM = VirtualKeyboardNMM;
