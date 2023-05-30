const main = async () => {
  const { IpcEvent } = ipc;
  // 等待其他模块的主动连接
  {
    jsProcess.onConnect((remoteIpc) => {
      remoteIpc.onMessage((message, ipc) => {
      });
    });
  }

  // 主动连接其他模块
  {
    setTimeout(async () => {
      const jsMMIpc = await jsProcess.connect("jmm.test.connect.dweb");
      jsMMIpc.onMessage((message, ipc) => {
      });

      jsMMIpc.onEvent((ipcEventMessage, ipc) => {
      });
      // name 必须是指定的模块名称
      // 发送消息必须要有模块的名称
      jsMMIpc.postMessage(IpcEvent.fromText("jmm.test.connect.dweb", "value"));
    }, 3000);
  }
};

main();
