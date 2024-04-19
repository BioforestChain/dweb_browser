import type { MicroModuleRuntime } from "../MicroModule";
import type { $IpcEvent } from "../ipc";

export const onSomeEvent = <T extends $IpcEvent>(
  runtime: MicroModuleRuntime,
  eventName: string,
  cb: (event: T) => unknown
) => {
  runtime.onConnect.collect((onConnectEvent) => {
    onConnectEvent.data.onEvent(`on-${eventName}`).collect((onIpcEventEvent) => {
      const ipcSomeEvent = onIpcEventEvent.consumeFilter<T>((event) => event.name === eventName);
      if (ipcSomeEvent !== undefined) {
        cb(ipcSomeEvent);
      }
    });
  });
};
