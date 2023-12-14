import { PromiseOut } from "../../helper/PromiseOut.ts";
import { ReadableStreamOut, streamRead } from "../../helper/stream/readableStreamHelper.ts";
import { IPC_DATA_ENCODING, Ipc, IpcEvent } from "./index.ts";

export class PureChannel {
  constructor(readonly income: ReadableStreamOut<$PureFrame>, readonly outgoing: ReadableStreamOut<$PureFrame>) {}
  private _startLock = new PromiseOut<void>();
  afterStart() {
    return this._startLock.promise;
  }
  start() {
    this._startLock.resolve();
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

export const enum PureFrameType {
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

export const ipcEventToPureFrame = (ipcEvent: IpcEvent) => {
  switch (ipcEvent.encoding) {
    case IPC_DATA_ENCODING.UTF8:
      return new PureTextFrame(ipcEvent.data as string);
    case IPC_DATA_ENCODING.BINARY:
      return new PureBinaryFrame(ipcEvent.data as Uint8Array);
  }
  throw new Error("invalid encoding to pure-frame");
};

export const pureFrameToIpcEvent = (eventName: string, pureFrame: $PureFrame) => {
  if (pureFrame.type === PureFrameType.Text) {
    return IpcEvent.fromText(eventName, pureFrame.data);
  }
  return IpcEvent.fromBinary(eventName, pureFrame.data);
};

export const pureChannelToIpcEvent = async (
  channelId: String,
  ipc: Ipc,
  pureChannel: PureChannel,
  /**收到ipcEvent时，需要对其进行接收的 channel*/
  channelWriteIn: ReadableStreamDefaultController<$PureFrame>,
  /**收到pureFrame时，需要将其转发给ipc的 channel*/
  channelReadOut: ReadableStream<$PureFrame>,
  waitReadyToStart: () => Promise<void> | void
) => {
  const eventStart = `${channelId}/start`;
  const eventData = `${channelId}/data`;
  const eventClose = `${channelId}/close`;
  const started = new PromiseOut<IpcEvent>();

  ipc.onEvent((ipcEvent) => {
    switch (ipcEvent.name) {
      case eventStart:
        started.resolve(ipcEvent);
        break;
      case eventData:
        channelWriteIn.enqueue(ipcEventToPureFrame(ipcEvent));
        break;
      case eventClose:
        pureChannel.close();
        break;
    }
  });
  ipc.postMessage(IpcEvent.fromText(eventStart, ""));

  void (async () => {
    ipc.postMessage(await started.promise);
    for await (const pureFrame of streamRead(channelReadOut)) {
      ipc.postMessage(pureFrameToIpcEvent(eventData, pureFrame));
    }
    // 关闭的时候，发一个信号给对面
    const ipcCloseEvent = IpcEvent.fromText(eventClose, "");
    ipc.postMessage(ipcCloseEvent);
  })();
};
