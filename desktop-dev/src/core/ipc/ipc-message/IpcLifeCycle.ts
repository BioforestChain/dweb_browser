import { $MicroModuleManifest } from "../../types.ts";
import { IPC_LIFECYCLE_STATE, ipcLifecycleStateBase } from "./internal/IpcLifecycle.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";

export type $IpcLifecycle<S extends $IpcLifecycleState = $IpcLifecycleState> = ReturnType<typeof ipcLifecycle<S>>;
export const ipcLifecycle = <S extends $IpcLifecycleState>(state: S) =>
  ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.LIFECYCLE), state } as const);

export type $IpcLifecycleState =
  | $IpcLifecycleInit
  | $IpcLifecycleOpening
  | $IpcLifecycleOpened
  | $IpcLifecycleClosing
  | $IpcLifecycleClosed;
export type $IpcLifecycleInit = ReturnType<typeof ipcLifecycleInit>;
export const ipcLifecycleInit = (pid: number, locale: $MicroModuleManifest, remote: $MicroModuleManifest) => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.INIT),
  pid,
  locale,
  remote,
});
export type $IpcLifecycleOpening = ReturnType<typeof ipcLifecycleOpening>;
export const ipcLifecycleOpening = () => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.OPENING),
});

export type $IpcLifecycleOpened = ReturnType<typeof ipcLifecycleOpened>;
export const ipcLifecycleOpened = () => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.OPENED),
});

export type $IpcLifecycleClosing = ReturnType<typeof ipcLifecycleClosing>;
export const ipcLifecycleClosing = (reason?: string) => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.CLOSING),
  reason,
});

export type $IpcLifecycleClosed = ReturnType<typeof ipcLifecycleClosed>;
export const ipcLifecycleClosed = (reason?: string) => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.CLOSED),
  reason,
});
