import type { $PromiseMaybe } from "@dweb-browser/helper/$PromiseMaybe.ts";
import { AdaptersManager } from "@dweb-browser/helper/fun/AdaptersManager.ts";
import type { MicroModule } from "./MicroModule.ts";
import type { Ipc } from "./ipc/ipc.ts";

export type $ConnectAdapter = (fromMM: MicroModule, toMM: MicroModule, reason: Request) => $PromiseMaybe<Ipc | void>;

export const connectAdapterManager = new AdaptersManager<$ConnectAdapter>();

// /** 外部程序与内部程序建立链接的方法 */
// export const connectMicroModules = async (fromMM: MicroModule, toMM: MicroModule, reason: Request) => {
//   for (const connectAdapter of connectAdapterManager.adapters) {
//     const ipc = await connectAdapter(fromMM, toMM, reason);
//     if (ipc != null) {
//       return ipc;
//     }
//   }
//   throw new Error(`no support connect MicroModules, from:${fromMM.mmid} to:${toMM.mmid}`);
// };
