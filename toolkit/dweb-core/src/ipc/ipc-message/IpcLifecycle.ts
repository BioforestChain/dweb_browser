import type { $MicroModuleManifest } from "../../types.ts";
import { IPC_LIFECYCLE_STATE, ipcLifecycleStateBase } from "./internal/IpcLifecycle.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";

export type $IpcLifecycleState =
  | $IpcLifecycleInit
  | $IpcLifecycleOpening
  | $IpcLifecycleOpened
  | $IpcLifecycleClosing
  | $IpcLifecycleClosed;
export type $IpcLifecycleInit = ReturnType<typeof IpcLifecycleInit>;
export const IpcLifecycleInit = (pid: number, locale: $MicroModuleManifest, remote: $MicroModuleManifest) => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.INIT),
  pid,
  locale,
  remote,
});
export type $IpcLifecycleOpening = ReturnType<typeof IpcLifecycleOpening>;
export const IpcLifecycleOpening = () => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.OPENING),
});

export type $IpcLifecycleOpened = ReturnType<typeof IpcLifecycleOpened>;
export const IpcLifecycleOpened = () => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.OPENED),
});

export type $IpcLifecycleClosing = ReturnType<typeof IpcLifecycleClosing>;
export const IpcLifecycleClosing = (reason?: string) => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.CLOSING),
  reason,
});

export type $IpcLifecycleClosed = ReturnType<typeof IpcLifecycleClosed>;
export const IpcLifecycleClosed = (reason?: string) => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.CLOSED),
  reason,
});

export type $IpcLifecycle<S extends $IpcLifecycleState = $IpcLifecycleState> = ReturnType<typeof ipcLifecycle<S>>;
const ipcLifecycle = <S extends $IpcLifecycleState>(state: S) =>
  ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.LIFECYCLE), state } as const);

export const IpcLifecycle = Object.assign(ipcLifecycle, {
  equals: (a: $IpcLifecycle, b: $IpcLifecycle) => {
    if (a.state.name !== b.state.name) {
      return false;
    }
    if (a.state.name === IPC_LIFECYCLE_STATE.CLOSING) {
      return a.state.reason === (b.state as $IpcLifecycleClosing).reason;
    }
    if (a.state.name === IPC_LIFECYCLE_STATE.CLOSED) {
      return a.state.reason === (b.state as $IpcLifecycleClosed).reason;
    }
    if (a.state.name === IPC_LIFECYCLE_STATE.INIT) {
      return JSON.stringify(a.state) === JSON.stringify(b.state);
    }
    return true;
  },
  init: IpcLifecycleInit,
  opening: IpcLifecycleOpening,
  opened: IpcLifecycleOpened,
  closing: IpcLifecycleClosing,
  closed: IpcLifecycleClosed,
});
