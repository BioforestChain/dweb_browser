import { PromiseOut } from "@dweb-browser/helper/PromiseOut.ts";
import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { ReadableStreamOut, streamRead } from "@dweb-browser/helper/stream/readableStreamHelper.ts";
import type { Ipc } from "../../ipc.ts";
import { IpcEvent, type $IpcEvent } from "../IpcEvent.ts";
import { IPC_DATA_ENCODING } from "../internal/IpcData.ts";

export const PURE_CHANNEL_EVENT_PREFIX = "§-";

export const INCOME = Symbol.for("pureChannel.income") as unknown as "pureChannel.income";
export const OUTGOING = Symbol.for("pureChannel.outgoing") as unknown as "pureChannel.outgoing";

export class PureChannel {
  constructor(income = new ReadableStreamOut<$PureFrame>(), outgoing = new ReadableStreamOut<$PureFrame>()) {
    this.#income = income;
    this.#outgoing = outgoing;
  }
  #income;
  #outgoing;
  private _startLock = new PromiseOut<void>();
  afterStart() {
    return this._startLock.promise;
  }
  start = $once(() => {
    this._startLock.resolve();
    const ctx = {
      [INCOME]: this.#income,
      [OUTGOING]: this.#outgoing,
      sendText: (text: string) => {
        ctx.send(new PureTextFrame(text));
      },
      sendBinary: (binary: Uint8Array) => {
        ctx.send(new PureBinaryFrame(binary));
      },
      send: (frame: $PureFrame) => {
        this.#outgoing.controller.enqueue(frame);
      },
      close: () => {
        this.close();
      },
    };
    return ctx;
  });
  close() {
    this.#income.controller.close();
    this.#outgoing.controller.close();
  }
  #reverse?: PureChannel;
  /**转换输入输出 */
  reverse() {
    if (this.#reverse === undefined) {
      const reverseChannel = new PureChannel(this.#outgoing, this.#income);
      reverseChannel.#reverse = this;
      this.#reverse = reverseChannel;
    }
    return this.#reverse;
  }
}

export enum PureFrameType {
  Text,
  Binary,
}
export type $PureFrame = PureTextFrame | PureBinaryFrame;
export class PureFrame<T extends PureFrameType> {
  constructor(readonly type: T) {}
}

export class PureTextFrame extends PureFrame<PureFrameType.Text> {
  constructor(readonly data: string) {
    super(PureFrameType.Text);
  }
}

export class PureBinaryFrame extends PureFrame<PureFrameType.Binary> {
  constructor(readonly data: Uint8Array) {
    super(PureFrameType.Binary);
  }
}

export const ipcEventToPureFrame = (event: $IpcEvent) => {
  switch (event.encoding) {
    case IPC_DATA_ENCODING.UTF8:
      return new PureTextFrame(event.data as string);
    case IPC_DATA_ENCODING.BINARY:
    case IPC_DATA_ENCODING.BASE64:
      return new PureBinaryFrame(IpcEvent.binary(event));
  }
};

export const pureFrameToIpcEvent = (eventName: string, pureFrame: $PureFrame, orderBy?: number) => {
  if (pureFrame.type === PureFrameType.Text) {
    return IpcEvent.fromText(eventName, pureFrame.data, orderBy);
  }
  return IpcEvent.fromBinary(eventName, pureFrame.data, orderBy);
};

export const pureChannelToIpcEvent = async (channelIpc: Ipc, pureChannel: PureChannel, debugTag: string) => {
  const eventData = `${PURE_CHANNEL_EVENT_PREFIX}data`;
  const orderBy = -1;

  const ipcListenToChannelPo = new PromiseOut<ReadableStreamDefaultController<$PureFrame>>();
  // 接收event消息。转换成PureFrame，发送到channel的income里面去
  channelIpc.onEvent("pureChannelToIpcEvent").collect(async (event) => {
    const ipcEvent = event.consumeMapNotNull((ipcEvent) => {
      if (ipcEvent.name === eventData) {
        return ipcEvent;
      }
    });
    if (ipcEvent === undefined) {
      return;
    }
    // 等待流控制器准备好再发送
    (await ipcListenToChannelPo.promise).enqueue(ipcEventToPureFrame(ipcEvent));
  });
  const ctx = pureChannel.start();
  // 拿到输入流控制器，开始接收ipc发送过来的ipcEvent事件，并且转换成pureFrame写入队列
  ipcListenToChannelPo.resolve(ctx[INCOME].controller);
  // 拿到输出流，准备进入ipc发送队列，转化成IpcEvent发送
  const channelReadOut = ctx[OUTGOING].stream;

  await channelIpc.start(undefined, debugTag);

  /// 将PureFrame转成IpcEvent，然后一同发给对面
  for await (const pureFrame of streamRead(channelReadOut)) {
    channelIpc.postMessage(pureFrameToIpcEvent(eventData, pureFrame, orderBy));
  }
};
