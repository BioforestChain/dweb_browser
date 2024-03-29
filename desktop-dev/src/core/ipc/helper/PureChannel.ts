import { PromiseOut } from "../../../helper/PromiseOut.ts";
import { ReadableStreamOut, streamRead } from "../../../helper/stream/readableStreamHelper.ts";
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
  const eventData = `${PURE_CHANNEL_EVENT_PREFIX}/data`;
  const orderBy = -1;

  const ipcListenToChannelPo = new PromiseOut<ReadableStreamDefaultController<$PureFrame>>();
  const off = channelIpc.onEvent("pureChannelToIpcEvent").collect(async (event) => {
    const ipcEvent = event.consumeMapNotNull((ipcEvent) => {
      if (ipcEvent.name === eventData) {
        return ipcEvent;
      }
    });
    if (ipcEvent === undefined) {
      return;
    }
    (await ipcListenToChannelPo.promise).enqueue(ipcEventToPureFrame(ipcEvent));
  });
  const ctx = pureChannel.start();
  ipcListenToChannelPo.resolve(ctx.incomeController);
  const channelReadOut = ctx.outgoingStream;

  await channelIpc.start(undefined, debugTag);

  /// 将PureFrame转成IpcEvent，然后一同发给对面
  for await (const pureFrame of streamRead(channelReadOut)) {
    channelIpc.postMessage(pureFrameToIpcEvent(eventData, pureFrame, orderBy));
  }
  off(); // 移除事件监听
};
