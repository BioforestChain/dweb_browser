import type { MicroModuleRuntime } from "../MicroModule.ts";
import type { Ipc } from "../ipc/index.ts";
import type { $IpcEvent } from "../ipc/ipc-message/IpcEvent.ts";

export const onSomeEvent = <T extends $IpcEvent>(
  runtime: MicroModuleRuntime,
  eventName: string,
  cb: (event: T) => unknown
) => {
  console.log("start onSomeEvent", eventName);
  const ipcOnEvent = (ipc: Ipc) => {
    void ipc.onEvent(`on-${eventName}`).collect((onIpcEventEvent) => {
      const ipcSomeEvent = onIpcEventEvent.consumeFilter<T>((event) => event.name === eventName);
      if (ipcSomeEvent !== undefined) {
        cb(ipcSomeEvent);
      }
    });
  };
  onAllIpc(runtime, `for-${eventName}`, ipcOnEvent);
};

export const onAllIpc = (runtime: MicroModuleRuntime, onConnectName: string, cb: (ipc: Ipc) => unknown) => {
  for (const ipc of runtime.connectedIpcs) {
    cb(ipc);
  }
  runtime.onConnect(onConnectName).collect((onConnectEvent) => {
    cb(onConnectEvent.consume());
  });
};
