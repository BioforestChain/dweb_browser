import type { MicroModuleRuntime } from "./MicroModule.ts";
import { onSomeEvent } from "./internal/ipcEventExt.ts";
import type { $IpcEvent } from "./ipc/ipc-message/IpcEvent.ts";

const ACTIVITY_EVENT = "activity";
export type $IpcActivityEvent = $IpcEvent<typeof ACTIVITY_EVENT>;

export const onActivity = (runtime: MicroModuleRuntime, cb: (event: $IpcActivityEvent) => unknown) => {
  return onSomeEvent(runtime, ACTIVITY_EVENT, cb);
};
