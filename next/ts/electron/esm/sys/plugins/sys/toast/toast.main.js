import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import { log } from "../../../../helper/devtools.js";
import { show } from "./handlers.js";
export class ToastNMM extends NativeMicroModule {
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
                log.green(`[${this.mmid} _bootstrap]`);
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/show",
                    matchMode: "full",
                    input: {},
                    output: "object",
                    handler: show.bind(this)
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
