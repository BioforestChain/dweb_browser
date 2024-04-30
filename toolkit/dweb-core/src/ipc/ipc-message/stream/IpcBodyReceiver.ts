import type { Ipc } from "../../ipc.ts";
import { BodyHub, IpcBody } from "./IpcBody.ts";
import type { MetaBody } from "./MetaBody.ts";

import { simpleEncoder } from "@dweb-browser/helper/encoding.ts";
import { IPC_DATA_ENCODING } from "../internal/IpcData.ts";
import { IPC_MESSAGE_TYPE } from "../internal/IpcMessage.ts";
import { ipcStreamAbort } from "./IpcStreamAbort.ts";
import { ipcStreamData } from "./IpcStreamData.ts";
import { ipcStreamPulling } from "./IpcStreamPulling.ts";

export class IpcBodyReceiver extends IpcBody {
  /**
   * 基于 metaBody 还原 IpcBodyReceiver
   */
  static from(metaBody: MetaBody, ipc: Ipc) {
    return (
      IpcBodyReceiver.CACHE.streamId_ipcBodySender_Map.get(metaBody.streamId) ?? new IpcBodyReceiver(metaBody, ipc)
    );
  }

  constructor(readonly metaBody: MetaBody, ipc: Ipc) {
    super();
    if (metaBody.type_isStream) {
      const streamId = metaBody.streamId!;
      if (IpcBodyReceiver.CACHE.streamId_receiverIpc_Map.has(streamId) === false) {
        ipc.onClosed(() => {
          IpcBodyReceiver.CACHE.streamId_receiverIpc_Map.delete(streamId);
        });
        IpcBodyReceiver.CACHE.streamId_receiverIpc_Map.set(streamId, ipc);
        metaBody.receiverUid = ipc.pool.poolId;
      }
      const receiver = IpcBodyReceiver.CACHE.streamId_receiverIpc_Map.get(streamId);
      if (receiver === undefined) {
        throw new Error(`no found ipc by streamId:${streamId}`);
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
          this._bodyHub = new BodyHub(simpleEncoder(metaBody.data as string, "base64"));
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

  /**
   * 默认是暂停状态
   */
  let paused = true;
  const stream = new ReadableStream<Uint8Array>(
    {
      start(controller) {
        /// ipc 一旦关闭，这个流也要关闭，因为只有这个ipc能为它提供数据
        ipc.onClosed(() => {
          try {
            controller.close();
            // deno-lint-ignore no-empty
          } catch {}
        });

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
        ipc.onStream("metaToStream").collect((event) => {
          const message = event.consumeMapNotNull((message) => {
            if (message.stream_id === stream_id) {
              return message;
            }
          });
          if (message === undefined) {
            return;
          }

          // STREAM_DATA || STREAM_END
          switch (message.type) {
            case IPC_MESSAGE_TYPE.STREAM_DATA:
              // console.log("receiver/data", stream_id, ipc.uid);
              controller.enqueue(ipcStreamData.binary(message));
              break;
            case IPC_MESSAGE_TYPE.STREAM_END:
              // console.log("receiver/end", stream_id, ipc.uid);
              controller.close();
              break;
          }
        });
      },
      pull(_controller) {
        if (paused) {
          paused = false;
          // console.log("receiver/pull", stream_id, ipc.uid);
          stream_ipc.postMessage(ipcStreamPulling(stream_id));
        }
      },
      cancel() {
        stream_ipc.postMessage(ipcStreamAbort(stream_id));
      },
    },
    {
      /// 按需 pull, 不可以0以上。否则一开始的时候就会发送pull指令，会导致远方直接把流给读取出来。
      /// 这会导致一些优化的行为异常，有些时候流一旦开始读取了，其他读取者就不能再进入读取了。那么流转发就不能工作了
      highWaterMark: 0,
    }
  );
  // console.log("receiver/start", stream_id, ipc.uid);
  return stream;
};

new WritableStream({});
