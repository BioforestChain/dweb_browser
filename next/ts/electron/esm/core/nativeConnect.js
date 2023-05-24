import { AdaptersManager } from "../helper/AdaptersManager.js";
import { NativeIpc } from "./ipc.native.js";
import { IPC_ROLE } from "./ipc/index.js";
export const connectAdapterManager = new AdaptersManager();
connectAdapterManager.append((fromMM, toMM, reason) => {
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
    const toNativeIpc = new NativeIpc(port1, fromMM, IPC_ROLE.SERVER);
    const fromNativeIpc = new NativeIpc(port2, toMM, IPC_ROLE.CLIENT);
    fromMM.beConnect(fromNativeIpc, reason); // 通知发起连接者作为Client
    toMM.beConnect(toNativeIpc, reason); // 通知接收者作为Server
    return [fromNativeIpc, toNativeIpc];
});
/** 外部程序与内部程序建立链接的方法 */
export const connectMicroModules = async (fromMM, toMM, reason) => {
    for (const connectAdapter of connectAdapterManager.adapters) {
        const ipc = await connectAdapter(fromMM, toMM, reason);
        if (ipc != null) {
            return ipc;
        }
    }
    throw new Error(`no support connect MicroModules, from:${fromMM.mmid} to:${toMM.mmid}`);
};
