import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import { log } from "../../../../helper/devtools.js";
import { getState, setState, startObserve, stopObserve } from "./handlers.js";
import { ipcMain } from "electron";
import { IpcEvent } from "../../../../core/ipc/index.js";
export class VirtualKeyboardNMM extends NativeMicroModule {
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
                log.green(`[${this.mmid} _bootstrap]`);
                {
                    // 监听从 multi-webview-comp-status-bar.html.mts 通过 ipcRenderer 发送过来的 监听数据
                    ipcMain.on("virtual_keyboard_state_change", (ipcMainEvent, host, statusbarState) => {
                        const b = this.observesState.get(host);
                        if (b === true) {
                            const ipc = this.observes.get(host);
                            if (ipc === undefined)
                                throw new Error(`ipc === undefined`);
                            ipc.postMessage(IpcEvent.fromText("observe", `${JSON.stringify(statusbarState)}`));
                        }
                    });
                }
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/getState",
                    matchMode: "full",
                    input: {},
                    output: "object",
                    handler: getState.bind(this),
                });
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/setState",
                    matchMode: "full",
                    input: {},
                    output: "object",
                    handler: setState.bind(this),
                });
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/startObserve",
                    matchMode: "full",
                    input: {},
                    output: "boolean",
                    handler: startObserve.bind(this),
                });
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/stopObserve",
                    matchMode: "full",
                    input: {},
                    output: "boolean",
                    handler: stopObserve.bind(this),
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
