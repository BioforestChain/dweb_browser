import { PromiseOut } from "../../helper/PromiseOut.ts";
import { ReadableStreamOut } from "../../helper/stream/readableStreamHelper.ts";
import { IPC_DATA_ENCODING, IpcEvent } from "./index.ts";

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
