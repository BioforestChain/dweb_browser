// src/user/toy/toy.worker.mts
var main = async () => {
  console.log("toy start");
  const { IpcEvent } = ipc;
  const cotIpc = await jsProcess.connect("cot.bfs.dweb");
  console.log("toy connect to", cotIpc.remote.mmid);
  cotIpc.onEvent((event, ipc2) => {
    console.log("got event:", ipc2.remote.mmid, event.name, event.text);
  });
  setInterval(() => {
    cotIpc.postMessage(IpcEvent.fromText("zzz", (/* @__PURE__ */ new Date()).toString()));
  }, 2e3);
};
main();
