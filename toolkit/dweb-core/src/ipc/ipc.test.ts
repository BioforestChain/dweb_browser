import { assert, test } from "vitest";

import { jsIpcPool } from "../ipc/IpcPool.ts";
import { IpcEvent } from "../ipc/ipc-message/IpcEvent.ts";
import type { $MicroModuleManifest } from "../types.ts";
import { WebMessageEndpoint } from "./endpoint/WebMessageEndpoint.ts";
import { IPC_MESSAGE_TYPE } from "./ipc-message/internal/IpcMessage.ts";

const clientManifest: $MicroModuleManifest = {
  mmid: "client.dweb",
  ipc_support_protocols: {
    cbor: false,
    protobuf: false,
    json: false,
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
    json: false,
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
      await port1.postMessage(IpcEvent.fromText("hi", item));
    }
    port1.close("send-done");
  })();

  const result = [] as string[];
  port2.onMessage("test").collect((event) => {
    const item = event.consumeMapNotNull((event) => {
      if (event.type === IPC_MESSAGE_TYPE.EVENT) {
        return IpcEvent.text(event);
      }
    });
    if (item !== undefined) {
      result.push(item);
    }
  });
  await port2.awaitClosed();
  console.log("result", result);
  assert.deepEqual(actual, result);
});
