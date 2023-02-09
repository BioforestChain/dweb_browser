import { streamRead } from "../../helper/readableStreamHelper.cjs";
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
  const reader = streamRead(stream);

  const sender = _postStreamData(stream_id, reader, ipc, () => {
    /// 解除对请求方的请求监听绑定
    off();
  });

  /// 来自请求方的数据
  const off = ipc.onMessage(async (message) => {
    /// 申请数据拉取
    if (
      message.type === IPC_DATA_TYPE.STREAM_PULL &&
      message.stream_id === stream_id
    ) {
      /// 预期值仅供参考
      // message.desiredSize
      await sender.next();
    }
    /// 告知数据流中断
    else if (
      message.type === IPC_DATA_TYPE.STREAM_ABORT &&
      message.stream_id === stream_id
    ) {
      reader.throw("abort");
    }
  });
};

/**
 * 这里的数据发送是按需迭代，而不是马上发
 * 马上发会有一定的问题，需要确保对方收到 IpcResponse 对象后，并且开始接收数据时才能开始
 * 否则发过去的数据 IpcResponse 如果还没构建完，就导致 IpcStreamData 无法认领，为了内存安全必然要被抛弃
 * 所以整体上来说，我们使用 pull 的逻辑，让远端来要求我们去发送数据
 */
async function* _postStreamData(
  stream_id: string,
  reader: AsyncGenerator<Uint8Array>,
  ipc: Ipc,
  onDone: () => unknown
) {
  for await (const data of reader) {
    ipc.postMessage(IpcStreamData.fromBinary(ipc, stream_id, data));
    yield;
  }
  /// 不论是不是被 aborted，都发送结束信号
  // if (reader.abort_controller.signal.aborted === false) {
  ipc.postMessage(new IpcStreamEnd(stream_id));
  // }

  onDone();
}
