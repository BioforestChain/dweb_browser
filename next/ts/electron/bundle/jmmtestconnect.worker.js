// src/user/jmm-test-connect/jmmtestconnect.worker.ts
var main = async () => {
  const { IpcEvent } = ipc;
  {
    jsProcess.onConnect((remoteIpc) => {
      console.log("jsmtestconnect.worker.mts onConnect");
      remoteIpc.onMessage((message, ipc2) => {
        console.log("jmtestconnect.worker.mct onmessage", message);
        setTimeout(() => {
          remoteIpc.postMessage(IpcEvent.fromText("\u8FD4\u56DE\u7684\u6D88\u606F key", "\u8FD4\u56DE\u7684\u6D88\u606Fvalue"));
        }, 1e3);
      });
      remoteIpc.onEvent((ipcEventMessage, ipc2) => {
        console.log("jmtestconnect.worker.mct onEvent", ipcEventMessage);
      });
    });
  }
};
main();
