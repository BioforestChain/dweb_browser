"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NativeMicroModule = void 0;
const micro_module_1 = require("./micro-module");
const native_ipc_1 = require("./native-ipc");
class NativeMicroModule extends micro_module_1.MicroModule {
    constructor() {
        super(...arguments);
        this._on_connect_cbs = new Set();
        this._channel = new MessageChannel();
        this.ipc = new native_ipc_1.NativeIpc(this._channel.port1);
        this.inner_ipc = new native_ipc_1.NativeIpc(this._channel.port2);
    }
    connect() {
        const channel = new MessageChannel();
        const { port1, port2 } = channel;
        for (const cb of this._on_connect_cbs) {
            const inner_ipc = new native_ipc_1.NativeIpc(port2);
            cb(inner_ipc);
        }
        return new native_ipc_1.NativeIpc(port1);
    }
    onConnect(cb) {
        this._on_connect_cbs.add(cb);
        return () => this._on_connect_cbs.delete(cb);
    }
}
exports.NativeMicroModule = NativeMicroModule;
