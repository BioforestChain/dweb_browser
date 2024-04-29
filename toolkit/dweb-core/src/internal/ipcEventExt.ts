import type { MicroModuleRuntime } from "../MicroModule.ts";
import type { Ipc } from "../ipc/index.ts";
import type { $IpcEvent } from "../ipc/ipc-message/IpcEvent.ts";

export const onSomeEvent = <T extends $IpcEvent>(
  runtime: MicroModuleRuntime,
  eventName: string,
  cb: (event: T) => unknown
) => {
  const ipcOnEvent = (ipc: Ipc) => {
    ipc.onEvent(`on-${eventName}`).collect((onIpcEventEvent) => {
      const ipcSomeEvent = onIpcEventEvent.consumeFilter<T>((event) => event.name === eventName);
      if (ipcSomeEvent !== undefined) {
        cb(ipcSomeEvent);
      }
    });
  };
  for (const ipc of runtime.connectedIpcs) {
    ipcOnEvent(ipc);
  }
  runtime.onConnect.collect((onConnectEvent) => {
    ipcOnEvent(onConnectEvent.consume());
  });
};
