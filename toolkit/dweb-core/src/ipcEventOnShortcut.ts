import type { MicroModuleRuntime } from "./MicroModule.ts";
import { onSomeEvent } from "./internal/ipcEventExt.ts";
import type { $IpcEvent } from "./ipc/ipc-message/IpcEvent.ts";

const SHORTCUT_EVENT = "shortcut";
export type $IpcShortcutEvent = $IpcEvent<typeof SHORTCUT_EVENT>;

export const onShortcut = (runtime: MicroModuleRuntime, cb: (event: $IpcShortcutEvent) => unknown) => {
  return onSomeEvent(runtime, SHORTCUT_EVENT, cb);
};
