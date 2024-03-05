import { concat } from "https://deno.land/std@0.140.0/bytes/mod.ts";
import type { $IpcRequest, $MMID, $MicroModuleManifest, $ReadableStreamIpc } from "../deps.ts";
import { PureBinaryFrame, ReadableStreamIpc, ReadableStreamOut, streamRead } from "../deps.ts";

type CreateDuplexIpcType = (subdomain: string, mmid: $MMID, ipcRequest: $IpcRequest, onClose: () => unknown) =>  $ReadableStreamIpc;

export const createDuplexIpc: CreateDuplexIpcType = (subdomain: string, mmid: $MMID, ipcRequest: $IpcRequest, onClose: () => unknown) => {
  const streamIpc = new ReadableStreamIpc(
    {
      mmid: mmid,
      name: `${subdomain}.${mmid}`,
      ipc_support_protocols: {
        cbor: false,
        protobuf: false,
        raw: false,
      },
      dweb_deeplinks: [],
      categories: [],
    } satisfies $MicroModuleManifest,
  );

  const incomeStream = new ReadableStreamOut<Uint8Array>();
  // 拿到自己前端的channel
  const pureServerChannel = ipcRequest.getChannel();
  pureServerChannel.start();

  // fetch(https://ext.dweb) => ipcRequest => streamIpc.request => streamIpc.postMessage => chunk => outgoing => ws.onMessage
  void (async () => {
    // 拿到网络层来的外部消息，发到前端处理
    for await (const chunk of streamRead(streamIpc.stream)) {
      pureServerChannel.outgoing.controller.enqueue(new PureBinaryFrame(concat(chunk)));
    }
  })();
  // ws.send => income.pureFrame =>
  void (async () => {
    //  绑定自己前端发送的数据通道
    for await (const pureFrame of streamRead(pureServerChannel.income.stream)) {
      if (pureFrame instanceof PureBinaryFrame) {
        incomeStream.controller.enqueue(pureFrame.data);
      }
    }
  })();

  void streamIpc.bindIncomeStream(incomeStream.stream).finally(() => {
    onClose();
  });
  return streamIpc;
};
