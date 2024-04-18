import { assert, test } from "vitest";

import { ipcEvent, jsIpcPool, type $MicroModuleManifest } from "../src/core/index.ts";
import { WebMessageEndpoint } from "../src/core/ipc/endpoint/WebMessageEndpoint.ts";
import { IPC_MESSAGE_TYPE } from "../src/core/ipc/ipc-message/internal/IpcMessage.ts";

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
  void port1.start(true, "then-test");
  void port2.start(true, "then-test");
  const MAX = 10;
  const actual = [] as string[];
  (async () => {
    for (let i = 1; i <= MAX; i++) {
      const item = `${i}`;
      actual.push(item);
      port1.postMessage(ipcEvent.fromText("hi", item));
    }
    port1.close();
  })();

  const result = [] as string[];
  port2.onMessage("test").collect((event) => {
    const item = event.consumeMapNotNull((event) => {
      if (event.type === IPC_MESSAGE_TYPE.EVENT) {
        return ipcEvent.text(event);
      }
    });
    if (item !== undefined) {
      result.push(item);
    }
  });
  await port2.awaitClosed();
  assert.deepEqual(actual, result);
});
