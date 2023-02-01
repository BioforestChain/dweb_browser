"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JsMicroModule = void 0;
const ipc_native_1 = require("./ipc.native");
const micro_module_1 = require("./micro-module");
class JsMicroModule extends micro_module_1.MicroModule {
    constructor() {
        super(...arguments);
        this._connectting_ipcs = new Set();
    }
    async before_bootstrap() {
        const process_id = (this._process_id = await this.fetch(`file://js.sys.dweb/create-procecss?main_code=${encodeURIComponent('console.log("worker running!", location.href)')}`).number());
    }
    connect() {
        const channel = new MessageChannel();
        const { port1, port2 } = channel;
        const inner_ipc = new ipc_native_1.NativeIpc(port2);
        this._connectting_ipcs.add(inner_ipc);
        inner_ipc.onClose(() => {
            this._connectting_ipcs.delete(inner_ipc);
        });
        for (const cb of this._on_connect_cbs) {
            cb(inner_ipc);
        }
        return new ipc_native_1.NativeIpc(port1);
    }
    after_shutdown() {
        super.after_shutdown();
        for (const inner_ipc of this._connectting_ipcs) {
            inner_ipc.close();
        }
        this._connectting_ipcs.clear();
    }
}
exports.JsMicroModule = JsMicroModule;
