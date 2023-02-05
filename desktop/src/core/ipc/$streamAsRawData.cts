import { simpleDecoder } from "../../helper/encoding.cjs";
import { IPC_DATA_TYPE } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";
import { IpcStreamData } from "./IpcStreamData.cjs";
import { IpcStreamEnd } from "./IpcStreamEnd.cjs";

/**
 * 如果 rawData 是流模式，需要提供数据发送服务
 *
 * 这里不会一直无脑发，而是对方有需要的时候才发
 * @param stream_id
 * @param stream
 * @param ipc
 */
export const $streamAsRawData = (
  stream_id: string,
  stream: ReadableStream<Uint8Array>,
  ipc: Ipc
) => {
  const reader = stream.getReader();
  /// 这里的数据发送是按需迭代，而不是马上发
  /// 马上发会有一定的问题，需要确保对方收到 IpcResponse 对象后，并且开始接收数据时才能开始
  /// 否则发过去的数据 IpcResponse 如果还没构建完，就导致 IpcStreamData 无法认领，为了内存安全必然要被抛弃
  /// 所以整体上来说，我们使用 pull 的逻辑，让远端来要求我们去发送数据
  const off = ipc.onMessage(async (message) => {
    if (
      message.type === IPC_DATA_TYPE.STREAM_PULL &&
      message.stream_id === stream_id
    ) {
      // let desiredSize = message.desiredSize;
      // while (desiredSize-- > 0) {}

      /// 预期值仅供参考
      console.log("desiredSize:", message.desiredSize);

      await sender.next();
    }
  });
  const sender = (async function* () {
    while (true) {
      yield;
      const item = await reader.read();
      if (item.done) {
        ipc.postMessage(new IpcStreamEnd(stream_id));
        break;
      } else {
        ipc.postMessage(
          new IpcStreamData(stream_id, simpleDecoder(item.value, "base64"))
        );
      }
    }

    /// 解除pull绑定
    off();
  })();
};
