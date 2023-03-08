const main = async () => {
  console.log("toy start");
  const { IpcEvent } = ipc;
//   {
//     const cotIpc = await jsProcess.connect("cot.bfs.dweb");
//     console.log("toy connected");
//     Object.assign(globalThis, { cotIpc });
//     cotIpc.onEvent((event, ipc) => {
//       console.log("got event:", ipc.remote.mmid, event.name, event.text);
//     });
//     setInterval(() => {
//       cotIpc.postMessage(IpcEvent.fromText("zzz", new Date().toString()));
//     }, 2000);
//   }
};

main();
