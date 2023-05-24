import type { $MicroModule } from "../helper/types.js";
import { MessagePortIpc } from "./ipc-web/MessagePortIpc.js";
import type { IPC_ROLE } from "./ipc/index.js";
export declare class NativeIpc extends MessagePortIpc {
    constructor(port: MessagePort, remote: $MicroModule, role: IPC_ROLE);
}
