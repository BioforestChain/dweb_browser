import { test } from "bun:test";
import { ipcEvent, jsIpcPool, type $MicroModuleManifest } from "../src/core/index.ts";
import { WebMessageEndpoint } from "../src/core/ipc/endpoint/WebMessageEndpoint.ts";

const clientManifest: $MicroModuleManifest = {
  mmid: "client.dweb",
  ipc_support_protocols: {
    cbor: false,
    protobuf: false,
    raw: false,
  },
  dweb_deeplinks: [],
  categories: [],
  name: "Client",
};
const serverManifest: $MicroModuleManifest = {
  mmid: "server.dweb",
  ipc_support_protocols: {
    cbor: false,
    protobuf: false,
    raw: false,
  },
  dweb_deeplinks: [],
  categories: [],
  name: "Server",
};

test("WebMessageEndpoint+IpcEvent", async () => {
  const channel = new MessageChannel();
  const endpoint1 = new WebMessageEndpoint(channel.port1, "port1");
  const endpoint2 = new WebMessageEndpoint(channel.port2, "port2");
  const port1 = jsIpcPool.createIpc(endpoint1, 0, clientManifest, serverManifest);
  const port2 = jsIpcPool.createIpc(endpoint2, 0, serverManifest, clientManifest);
  await port1.start();
  (async () => {
    for (let i = 0; i < 10; i++) {
      port1.postMessage(ipcEvent.fromText("hi", "${i}"));
    }
  })();

  port2.onMessage("test").collect((event) => {
    console.log(event.consume());
  });
});
