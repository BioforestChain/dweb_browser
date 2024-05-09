import { PromiseOut } from "@dweb-browser/helper/PromiseOut.ts";
import { ReadableStreamOut, streamRead } from "@dweb-browser/helper/stream/readableStreamHelper.ts";
import { IpcEvent, type $IpcEvent } from "../ipc-message/IpcEvent.ts";
import { PURE_CHANNEL_EVENT_PREFIX } from "../ipc-message/IpcRequest.ts";
import { IPC_DATA_ENCODING } from "../ipc-message/internal/IpcData.ts";
import type { Ipc } from "../ipc.ts";

export class PureChannel {
  constructor(
    readonly income = new ReadableStreamOut<$PureFrame>(),
    readonly outgoing = new ReadableStreamOut<$PureFrame>()
  ) {}
  private _startLock = new PromiseOut<void>();
  afterStart() {
    return this._startLock.promise;
  }
  start() {
    this._startLock.resolve();
    return {
      incomeController: this.income.controller,
      outgoingStream: this.outgoing.stream,
    };
  }
  close() {
    this.income.controller.close();
    this.outgoing.controller.close();
  }
  private _reverse?: PureChannel;
  reverse() {
    if (this._reverse === undefined) {
      this._reverse = new PureChannel(this.outgoing, this.income);
      this._reverse._reverse = this;
    }
    return this._reverse;
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
  ipcListenToChannelPo.resolve(ctx.incomeController);
  // 拿到输出流，准备进入ipc发送队列，转化成IpcEvent发送
  const channelReadOut = ctx.outgoingStream;

  await channelIpc.start(undefined, debugTag);

  /// 将PureFrame转成IpcEvent，然后一同发给对面
  for await (const pureFrame of streamRead(channelReadOut)) {
    channelIpc.postMessage(pureFrameToIpcEvent(eventData, pureFrame, orderBy));
  }
};
