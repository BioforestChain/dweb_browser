export * from "../../deps.ts";

export const { jsProcess, http, ipc } = navigator.dweb;

export const { ServerUrlInfo, ServerStartResult } = http;
export type $ServerUrlInfo = InstanceType<typeof ServerUrlInfo>;
export type $ServerStartResult = InstanceType<typeof ServerStartResult>;

export const { IpcHeaders, IpcResponse, Ipc, IpcRequest, IpcEvent, IPC_METHOD } = ipc;
export type $Ipc = InstanceType<typeof Ipc>;
export type $IpcRequest = InstanceType<typeof IpcRequest>;
export type $IpcResponse = InstanceType<typeof IpcResponse>;
export type $IpcEvent = InstanceType<typeof IpcEvent>;
