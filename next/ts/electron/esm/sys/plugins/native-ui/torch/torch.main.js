import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import { log } from "../../../../helper/devtools.js";
import { toggleTorch, torchState } from "./handlers.js";
export class TorchNMM extends NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "torch.nativeui.sys.dweb"
        });
        Object.defineProperty(this, "_bootstrap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (context) => {
                log.green(`[${this.mmid} _bootstrap]`);
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/toggleTorch",
                    matchMode: "full",
                    input: {},
                    output: "object",
                    handler: toggleTorch.bind(this)
                });
                this.registerCommonIpcOnMessageHandler({
                    pathname: "/torchState",
                    matchMode: "full",
                    input: {},
                    output: "object",
                    handler: torchState.bind(this)
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
