import type { $IpcMicroModuleInfo, $IpcSupportProtocols } from "../../helper/types.js";
import { $IpcMessage, IPC_ROLE } from "../ipc/const.js";
import { Ipc } from "../ipc/ipc.js";
export declare class MessagePortIpc extends Ipc {
    readonly port: MessagePort;
    readonly remote: $IpcMicroModuleInfo;
    readonly role: IPC_ROLE;
    readonly self_support_protocols: $IpcSupportProtocols;
    constructor(port: MessagePort, remote: $IpcMicroModuleInfo, role?: IPC_ROLE, self_support_protocols?: $IpcSupportProtocols);
    _doPostMessage(message: $IpcMessage): void;
    _doClose(): void;
}
