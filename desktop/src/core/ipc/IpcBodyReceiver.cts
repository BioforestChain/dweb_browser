import { $MetaBody, $metaBodyToBinary } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";
import { BodyHub, IpcBody } from "./IpcBody.cjs";

import { IPC_MESSAGE_TYPE, IPC_META_BODY_TYPE } from "./const.cjs";
import { IpcStreamPull } from "./IpcStreamPull.cjs";

export class IpcBodyReceiver extends IpcBody {
  private static metaIdIpcMap = new Map<string, Ipc>();

  constructor(readonly metaBody: $MetaBody, ipc: Ipc) {
    super();
    switch (metaBody[0]) {
      case IPC_META_BODY_TYPE.STREAM_ID:
        {
          const streamId = metaBody[1] as string;
          const senderIpcUid = metaBody[2];
          const metaId = `${senderIpcUid}/${streamId}`;
          if (IpcBodyReceiver.metaIdIpcMap.has(metaId) === false) {
            ipc.onClose(() => {
              IpcBodyReceiver.metaIdIpcMap.delete(metaId);
            });
            IpcBodyReceiver.metaIdIpcMap.set(metaId, ipc);
          }
          const receiver = IpcBodyReceiver.metaIdIpcMap.get(metaId);
          if (receiver === undefined) {
            throw new Error(`no found ipc by metaId:${metaId}`);
          }
          ipc = receiver;
          this._bodyHub = new BodyHub($metaToStream(this.metaBody, ipc));
        }
        break;
      /// 文本模式，直接返回即可，因为 RequestInit/Response 支持支持传入 utf8 字符串
      case IPC_META_BODY_TYPE.TEXT:
        {
          this._bodyHub = new BodyHub(metaBody[1] as string);
        }
        break;
      default:
        {
          this._bodyHub = new BodyHub($metaBodyToBinary(metaBody));
        }
        break;
    }
  }
  protected _bodyHub: BodyHub;
}
const $metaToStream = (rawBody: $MetaBody, ipc: Ipc) => {
  if (ipc == null) {
    throw new Error(`miss ipc when ipc-response has stream-body`);
  }
  const stream_ipc = ipc;
  const stream_id = rawBody[1] as string;
  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      const off = ipc.onMessage((message) => {
        if ("stream_id" in message && message.stream_id === stream_id) {
          if (message.type === IPC_MESSAGE_TYPE.STREAM_DATA) {
            console.log("getStreamDataMessage", stream_id);
            controller.enqueue(message.binary);
          } else if (message.type === IPC_MESSAGE_TYPE.STREAM_END) {
            controller.close();
            off();
          }
        }
      });
    },
    pull(controller) {
      console.log("postStreamPullMessage", stream_id, controller.desiredSize);
      stream_ipc.postMessage(
        new IpcStreamPull(stream_id, controller.desiredSize)
      );
    },
  });
  return stream;
};
