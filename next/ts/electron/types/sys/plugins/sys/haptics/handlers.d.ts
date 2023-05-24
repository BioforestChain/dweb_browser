import type { Ipc, IpcRequest } from "../../../../core/ipc/index.js";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.js";
import type { HapticsNMM } from "./haptics.main.js";
export declare function setHaptics(this: HapticsNMM, args: $Schema1ToType<$Schema1>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<boolean>;
