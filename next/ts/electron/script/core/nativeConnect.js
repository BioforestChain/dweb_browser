"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.connectMicroModules = exports.connectAdapterManager = void 0;
const AdaptersManager_js_1 = require("../helper/AdaptersManager.js");
const ipc_native_js_1 = require("./ipc.native.js");
const index_js_1 = require("./ipc/index.js");
exports.connectAdapterManager = new AdaptersManager_js_1.AdaptersManager();
exports.connectAdapterManager.append((fromMM, toMM, reason) => {
    // // 原始代码
    // if (toMM instanceof NativeMicroModule) {
    //   const channel = new MessageChannel();
    //   const { port1, port2 } = channel;
    //   const toNativeIpc = new NativeIpc(port1, fromMM, IPC_ROLE.SERVER);
    //   const fromNativeIpc = new NativeIpc(port2, toMM, IPC_ROLE.CLIENT);
    //   fromMM.beConnect(fromNativeIpc, reason); // 通知发起连接者作为Client
    //   toMM.beConnect(toNativeIpc, reason); // 通知接收者作为Server
    //   return [fromNativeIpc, toNativeIpc];
    // }
    // 测试代码
    const channel = new MessageChannel();
    const { port1, port2 } = channel;
    const toNativeIpc = new ipc_native_js_1.NativeIpc(port1, fromMM, index_js_1.IPC_ROLE.SERVER);
    const fromNativeIpc = new ipc_native_js_1.NativeIpc(port2, toMM, index_js_1.IPC_ROLE.CLIENT);
    fromMM.beConnect(fromNativeIpc, reason); // 通知发起连接者作为Client
    toMM.beConnect(toNativeIpc, reason); // 通知接收者作为Server
    return [fromNativeIpc, toNativeIpc];
});
/** 外部程序与内部程序建立链接的方法 */
const connectMicroModules = async (fromMM, toMM, reason) => {
    for (const connectAdapter of exports.connectAdapterManager.adapters) {
        const ipc = await connectAdapter(fromMM, toMM, reason);
        if (ipc != null) {
            return ipc;
        }
    }
    throw new Error(`no support connect MicroModules, from:${fromMM.mmid} to:${toMM.mmid}`);
};
exports.connectMicroModules = connectMicroModules;
