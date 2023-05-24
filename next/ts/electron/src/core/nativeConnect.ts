import { AdaptersManager } from "../helper/AdaptersManager.js";
import type { $PromiseMaybe } from "../helper/types.js";
import { NativeIpc } from "./ipc.native.js";
import { IPC_ROLE } from "./ipc/index.js";
import type { Ipc } from "./ipc/ipc.js";
import type { MicroModule } from "./micro-module.js";

/**
 * 两个模块的连接结果：
 *
 * 1. fromIpc 是肯定有的，这个对象是我们当前的上下文发起连接得来的通道，要与 toMM 通讯都需要通过它
 * 1. toIpc 则不一定，远程模块可能是自己创建了 Ipc，我们的上下文拿不到这个内存对象
 */
export type $ConnectResult = [Ipc, Ipc | undefined];

export type $ConnectAdapter = (
  fromMM: MicroModule,
  toMM: MicroModule,
  reason: Request
) => $PromiseMaybe<$ConnectResult | void>;

export const connectAdapterManager = new AdaptersManager<$ConnectAdapter>();

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
export const connectMicroModules = async (
  fromMM: MicroModule,
  toMM: MicroModule,
  reason: Request
) => {
  for (const connectAdapter of connectAdapterManager.adapters) {
    const ipc = await connectAdapter(fromMM, toMM, reason);
    if (ipc != null) {
      return ipc;
    }
  }
  throw new Error(
    `no support connect MicroModules, from:${fromMM.mmid} to:${toMM.mmid}`
  );
};
