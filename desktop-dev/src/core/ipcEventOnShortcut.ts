import type { MicroModuleRuntime } from "./MicroModule.ts";
import type { $IpcEvent } from "./ipc/ipc-message/IpcEvent.ts";

const SHORTCUT_EVENT = "shortcut";
export type $IpcShortcutEvent = $IpcEvent<typeof SHORTCUT_EVENT>;

export const onShortcut = (runtime: MicroModuleRuntime, cb: (event: $IpcShortcutEvent) => unknown) => {
  runtime.onConnect.collect((onConnectEvent) => {
    onConnectEvent.data.onEvent("onShortcut").collect((onIpcEventEvent) => {
      const ipcRendererStartEvent = onIpcEventEvent.consumeFilter<$IpcShortcutEvent>(
        (event) => event.name === SHORTCUT_EVENT
      );
      if (ipcRendererStartEvent !== undefined) {
        cb(ipcRendererStartEvent);
      }
    });
  });
};
