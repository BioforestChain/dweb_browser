import type { MicroModuleRuntime } from "./MicroModule.ts";
import { onSomeEvent } from "./internal/ipcEventExt.ts";
import type { $IpcEvent } from "./ipc/ipc-message/IpcEvent.ts";

const enum RENDERER_EVENT {
  START = "renderer",
  DESTROY = "renderer-destroy",
}
export type $IpcRendererStartEvent = $IpcEvent<RENDERER_EVENT.START>;
export const onRenderer = (runtime: MicroModuleRuntime, cb: (event: $IpcRendererStartEvent) => unknown) => {
  return onSomeEvent(runtime, RENDERER_EVENT.START, cb);
};
export type $IpcRendererDestroyEvent = $IpcEvent<RENDERER_EVENT.DESTROY>;
export const onRendererDestroy = (runtime: MicroModuleRuntime, cb: (event: $IpcRendererDestroyEvent) => unknown) => {
  return onSomeEvent(runtime, RENDERER_EVENT.DESTROY, cb);
};
