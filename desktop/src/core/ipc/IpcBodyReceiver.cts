import type { Ipc } from "./ipc.cjs";
import { BodyHub, IpcBody } from "./IpcBody.cjs";
import type { MetaBody } from "./MetaBody.cjs";

import { simpleEncoder } from "../../helper/encoding.cjs";
import { IPC_DATA_ENCODING, IPC_MESSAGE_TYPE } from "./const.cjs";
import { IpcStreamPull } from "./IpcStreamPull.cjs";

export class IpcBodyReceiver extends IpcBody {
  private static metaIdIpcMap = new Map<string, Ipc>();

  constructor(readonly metaBody: MetaBody, ipc: Ipc) {
    super();
    if (metaBody.type_isStream) {
      const streamId = metaBody.streamId!;
      const senderIpcUid = metaBody.senderUid;
      const metaId = `${senderIpcUid}/${streamId}`;
      /// 将第一次得到这个metaBody的 ipc 保存起来，这个ipc将用于接收
      if (IpcBodyReceiver.metaIdIpcMap.has(metaId) === false) {
        ipc.onClose(() => {
          IpcBodyReceiver.metaIdIpcMap.delete(metaId);
        });
        IpcBodyReceiver.metaIdIpcMap.set(metaId, ipc);
        metaBody.receiverUid = ipc.uid;
      }
      const receiver = IpcBodyReceiver.metaIdIpcMap.get(metaId);
      if (receiver === undefined) {
        throw new Error(`no found ipc by metaId:${metaId}`);
      }
      ipc = receiver;
      this._bodyHub = new BodyHub($metaToStream(this.metaBody, ipc));
    } else
      switch (metaBody.type_encoding) {
        case IPC_DATA_ENCODING.UTF8:
          /// 文本模式，直接返回即可，因为 RequestInit/Response 支持支持传入 utf8 字符串
          this._bodyHub = new BodyHub(metaBody.data as string);
          break;
        case IPC_DATA_ENCODING.BASE64:
          this._bodyHub = new BodyHub(
            simpleEncoder(metaBody.data as string, "base64")
          );
          break;
        case IPC_DATA_ENCODING.BINARY:
          this._bodyHub = new BodyHub(metaBody.data as Uint8Array);
          break;
        default:
          throw new Error(`invalid metaBody type: ${metaBody.type}`);
      }
  }
  protected _bodyHub: BodyHub;
}
const $metaToStream = (metaBody: MetaBody, ipc: Ipc) => {
  if (ipc == null) {
    throw new Error(`miss ipc when ipc-response has stream-body`);
  }
  const stream_ipc = ipc;
  const stream_id = metaBody.streamId!;
  const stream = new ReadableStream<Uint8Array>(
    {
      start(controller) {
        /// 如果有初始帧，直接存起来
        let firstData: undefined | Uint8Array;
        switch (metaBody.type_encoding) {
          case IPC_DATA_ENCODING.UTF8:
            firstData = simpleEncoder(metaBody.data as string, "utf8");
            break;
          case IPC_DATA_ENCODING.BASE64:
            firstData = simpleEncoder(metaBody.data as string, "base64");
            break;
          case IPC_DATA_ENCODING.BINARY:
            firstData = metaBody.data as Uint8Array;
            break;
        }
        if (firstData) {
          controller.enqueue(firstData);
        }

        /// 监听事件
        const off = ipc.onMessage((message) => {
          if ("stream_id" in message && message.stream_id === stream_id) {
            if (message.type === IPC_MESSAGE_TYPE.STREAM_DATA) {
              controller.enqueue(message.binary);
            } else if (message.type === IPC_MESSAGE_TYPE.STREAM_END) {
              controller.close();
              off();
            }
          }
        });
      },
      pull(controller) {
        stream_ipc.postMessage(
          new IpcStreamPull(stream_id, controller.desiredSize)
        );
      },
    },
    {
      /// 按需 pull
      highWaterMark: 0,
    }
  );
  return stream;
};
