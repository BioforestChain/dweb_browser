/**ipc生命周期 */
export const enum IPC_LIFECYCLE_STATE {
  INIT = "init",
  OPENING = "opening",
  OPENED = "opened",
  CLOSING = "closing",
  CLOSED = "closed",
}

export const ipcLifecycleStateBase = <S extends IPC_LIFECYCLE_STATE>(state: S) => ({ state } as const);
