import { assert, test } from "vitest";

import { Channel } from "@dweb-browser/helper/Channel.ts";
import { streamRead } from "@dweb-browser/helper/stream/readableStreamHelper.ts";
import { jsIpcPool } from "../ipc/IpcPool.ts";
import { IpcEvent } from "../ipc/ipc-message/IpcEvent.ts";
import type { $MicroModuleManifest } from "../types.ts";
import { WebMessageEndpoint } from "./endpoint/WebMessageEndpoint.ts";
import { IpcHeaders } from "./helper/IpcHeaders.ts";
import { IpcResponse } from "./index.ts";
import { IPC_MESSAGE_TYPE } from "./ipc-message/internal/IpcMessage.ts";

export const clientManifest: $MicroModuleManifest = {
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
export const serverManifest: $MicroModuleManifest = {
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

/**
 * 测试channel 转换
 */
test("channel", async () => {
  const channel = new MessageChannel();
  const endpoint1 = new WebMessageEndpoint(channel.port1, "port1");
  const endpoint2 = new WebMessageEndpoint(channel.port2, "port2");
  const expected: string[] = [];
  (async () => {
    const clientIpc = jsIpcPool.createIpc(endpoint1, 0, clientManifest, serverManifest, true);
    const reqHeaders = new IpcHeaders();
    const channelIpc = await clientIpc.prepareChannel(reqHeaders);
    clientIpc.request("file://forward.dweb/connect", {
      headers: reqHeaders,
    });

    channelIpc.start();
    channelIpc.onEvent("read-pure-channel-data-by-ipc-event").collect((event) => {
      const data = IpcEvent.text(event.consume());
      console.log("in", data);
      if (data === "close") {
        return clientIpc.close();
      }
      expected.push(data);
    });
  })();
  const actual: string[] = [];
  {
    const serverIpc = jsIpcPool.createIpc(endpoint2, 0, serverManifest, clientManifest, true);
    serverIpc.onRequest("xxx").collect((requestEvent) => {
      const serverRequest = requestEvent.consume();
      console.log("serverRequest", serverRequest);
      assert.isTrue(serverRequest.hasDuplex);
      const channel = serverRequest.getChannel();
      const ctx = channel.start();
      for (let i = 0; i < 10; i++) {
        const data = `${i}`;
        actual.push(data);
        ctx.sendText(data);
      }
      ctx.sendText("close");
    });
    await serverIpc.awaitClosed();
  }

  await assert.deepEqual(actual, expected);
});

test("channel-forward", async () => {
  const channel1 = new MessageChannel();
  const endpoint1 = new WebMessageEndpoint(channel1.port1, "port1");
  const endpoint2 = new WebMessageEndpoint(channel1.port2, "port2");
  const channel2 = new MessageChannel();
  const endpoint3 = new WebMessageEndpoint(channel2.port1, "port3");
  const endpoint4 = new WebMessageEndpoint(channel2.port2, "port4");
  const ipc1 = jsIpcPool.createIpc(endpoint1, 0, clientManifest, serverManifest, true);
  const ipc2 = jsIpcPool.createIpc(endpoint2, 0, serverManifest, clientManifest, true);
  const ipc3 = jsIpcPool.createIpc(endpoint3, 0, clientManifest, serverManifest, true);
  const ipc4 = jsIpcPool.createIpc(endpoint4, 0, serverManifest, clientManifest, true);

  const expected: string[] = [];
  const actual: string[] = [];

  (async () => {
    const reqHeaders = new IpcHeaders();
    await ipc1.prepareChannel(reqHeaders);
    const client = await ipc1.request("file://forward.dweb/connect", {
      headers: reqHeaders,
    });
    (async () => {
      for await (const data of streamRead(await client.body.stream())) {
        const text = new TextDecoder().decode(data);
        console.log(`收到消息=>${text}`);
        if (text === "close") {
          return ipc1.close();
        }
        expected.push(text);
      }
    })();
  })();
  {
    // 转发消息
    ipc2.onRequest("xxx-request-ipc2").collect(async (requestEvent) => {
      const serverRequest = requestEvent.consume();
      assert.isTrue(serverRequest.hasDuplex);
      const request = serverRequest.toPureClinetRequest();
      // 转换完发给3
      const response = await ipc3.request(request.url, request);
      // 回给2
      ipc2.postMessage(await IpcResponse.fromResponse(serverRequest.reqId, response.toResponse(), ipc2));
    });
  }
  {
    // 4最终处理
    ipc4.onRequest("xxx-request-ipc4").collect((requestEvent) => {
      const req = requestEvent.consume();
      const channel = new Channel<Uint8Array>();
      ipc4.postMessage(IpcResponse.fromStream(req.reqId, 200, undefined, channel.stream, ipc4));
      for (let i = 0; i < 10; i++) {
        const data = `${i}`;
        actual.push(data);
        channel.send(new TextEncoder().encode(data));
      }
      channel.send(new TextEncoder().encode("close"));
      ipc4.close();
    });
  }
  await ipc1.awaitClosed();
  await ipc2.awaitClosed();
  await ipc3.awaitClosed();
  await ipc4.awaitClosed();
  assert.deepEqual(actual, expected);
});
