import { simpleEncoder } from "../../helper/encoding.cjs";
import { $RawData, IPC_DATA_TYPE, IPC_RAW_BODY_TYPE } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";
import { IpcStreamPull } from "./IpcStreamPull.cjs";

export const $rawDataToBody = (rawBody: $RawData, ipc?: Ipc) => {
  let body: string | Uint8Array | ReadableStream<Uint8Array>;

  const raw_body_type = rawBody[0];
  const bodyEncoder =
    raw_body_type & IPC_RAW_BODY_TYPE.BINARY
      ? (data: unknown) => data as Uint8Array
      : raw_body_type & IPC_RAW_BODY_TYPE.BASE64
      ? (data: unknown) => simpleEncoder(data as string, "base64")
      : raw_body_type & IPC_RAW_BODY_TYPE.TEXT
      ? (data: unknown) => simpleEncoder(data as string, "utf8")
      : () => {
          throw raw_body_type;
        };

  if (raw_body_type & IPC_RAW_BODY_TYPE.STREAM_ID) {
    if (ipc == null) {
      throw new Error(`miss ipc when ipc-response has stream-body`);
    }
    const stream_ipc = ipc;
    const stream_id = rawBody[1] as string;
    body = new ReadableStream<Uint8Array>({
      start(controller) {
        const off = ipc.onMessage((message) => {
          if ("stream_id" in message && message.stream_id === stream_id) {
            if (message.type === IPC_DATA_TYPE.STREAM_DATA) {
              controller.enqueue(
                typeof message.data === "string"
                  ? bodyEncoder(message.data)
                  : message.data
              );
            } else if (message.type === IPC_DATA_TYPE.STREAM_END) {
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
    });
  } else {
    body = /// 文本模式，直接返回即可，因为 RequestInit/Response 支持支持传入 utf8 字符串
      raw_body_type & IPC_RAW_BODY_TYPE.TEXT
        ? rawBody[1]
        : /// 其它模式
          bodyEncoder(rawBody[1]);
  }
  return body;
};
