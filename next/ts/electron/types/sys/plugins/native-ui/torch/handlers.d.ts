import type { Ipc, IpcRequest } from "../../../../core/ipc/index.js";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.js";
import type { TorchNMM } from "./torch.main.js";
export declare function toggleTorch(this: TorchNMM, args: $Schema1ToType<$Schema1>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<Response>;
export declare function torchState(this: TorchNMM, args: $Schema1ToType<{}>, client_ipc: Ipc, ipcRequest: IpcRequest): Promise<Response>;
