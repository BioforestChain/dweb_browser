import { AdaptersManager } from "../helper/AdaptersManager.ts";
import type { $PromiseMaybe } from "./helper/types.ts";
import type { Ipc } from "./ipc/ipc.ts";
import type { MicroModule } from "./micro-module.ts";

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

/** 外部程序与内部程序建立链接的方法 */
export const connectMicroModules = async (fromMM: MicroModule, toMM: MicroModule, reason: Request) => {
  for (const connectAdapter of connectAdapterManager.adapters) {
    const ipc = await connectAdapter(fromMM, toMM, reason);
    if (ipc != null) {
      return ipc;
    }
  }
  throw new Error(`no support connect MicroModules, from:${fromMM.mmid} to:${toMM.mmid}`);
};
