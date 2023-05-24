// src/user/jmm-test-connect2/jmmtestconnect2.worker.ts
var main = async () => {
  const { IpcEvent } = ipc;
  {
    jsProcess.onConnect((remoteIpc) => {
      console.log("jsmtestconnect-2.worker.mts onConnect");
      remoteIpc.onMessage((message, ipc2) => {
        console.log("jmtestconnect-2.worker.mct onmessage", message);
      });
    });
  }
  {
    setTimeout(async () => {
      const jsMMIpc = await jsProcess.connect("jmm.test.connect.dweb");
      jsMMIpc.onMessage((message, ipc2) => {
        console.log("jmm.text.connect.2.worker.mts \u63A5\u53D7\u5230\u4E86\u6D88\u606F", message);
      });
      jsMMIpc.onEvent((ipcEventMessage, ipc2) => {
        console.log("jmm.text.connect.2.worker.mts onEvent", ipcEventMessage);
      });
      jsMMIpc.postMessage(IpcEvent.fromText("jmm.test.connect.dweb", "value"));
    }, 3e3);
  }
};
main();
