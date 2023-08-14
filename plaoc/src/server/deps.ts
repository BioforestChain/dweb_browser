export type * from "dweb/browser/js-process/assets/worker/std-dweb-core.ts";
export type * from "dweb/browser/js-process/assets/worker/std-dweb-http.ts";
export * from "../../server.deps.ts";

export const { jsProcess, http, ipc, core } = navigator.dweb;

export const { ServerUrlInfo, ServerStartResult } = http;
export type $ServerUrlInfo = InstanceType<typeof ServerUrlInfo>;
export type $ServerStartResult = InstanceType<typeof ServerStartResult>;

export const {
  //
  IpcHeaders,
  IpcResponse,
  Ipc,
  IpcRequest,
  IpcEvent,
  IPC_METHOD,
  ReadableStreamIpc,
  ReadableStreamOut,
} = ipc;
export type $Ipc = InstanceType<typeof Ipc>;
export type $IpcRequest = InstanceType<typeof IpcRequest>;
export type $IpcResponse = InstanceType<typeof IpcResponse>;
export type $IpcEvent = InstanceType<typeof IpcEvent>;
export type $ReadableStreamIpc = InstanceType<typeof ReadableStreamIpc>;
export type $ReadableStreamOut<T> = InstanceType<typeof ReadableStreamOut<T>>;

export const { FetchError, IPC_ROLE } = core;
export type $FetchError = InstanceType<typeof FetchError>;
