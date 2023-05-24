import { AdaptersManager } from "../helper/AdaptersManager.js";
import type { $PromiseMaybe } from "../helper/types.js";
import type { Ipc } from "./ipc/ipc.js";
import type { MicroModule } from "./micro-module.js";
/**
 * 两个模块的连接结果：
 *
 * 1. fromIpc 是肯定有的，这个对象是我们当前的上下文发起连接得来的通道，要与 toMM 通讯都需要通过它
 * 1. toIpc 则不一定，远程模块可能是自己创建了 Ipc，我们的上下文拿不到这个内存对象
 */
export type $ConnectResult = [Ipc, Ipc | undefined];
export type $ConnectAdapter = (fromMM: MicroModule, toMM: MicroModule, reason: Request) => $PromiseMaybe<$ConnectResult | void>;
export declare const connectAdapterManager: AdaptersManager<$ConnectAdapter>;
/** 外部程序与内部程序建立链接的方法 */
export declare const connectMicroModules: (fromMM: MicroModule, toMM: MicroModule, reason: Request) => Promise<$ConnectResult>;
