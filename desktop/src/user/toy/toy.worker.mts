const main = async () => {
  console.log("toy start");
  const { IpcEvent } = ipc;
  const cotIpc = await jsProcess.connect("cot.bfs.dweb");
  console.log("toy connect to", cotIpc.remote.mmid);

  cotIpc.onEvent((event, ipc) => {
    console.log("got event:", ipc.remote.mmid, event.name, event.text);
  });
  setInterval(() => {
    cotIpc.postMessage(IpcEvent.fromText("zzz", new Date().toString()));
  }, 2000);
};

main();
