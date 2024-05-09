import { ReadableStreamOut, streamRead } from "./stream/readableStreamHelper.ts";

/**模拟kotlin Channel */
export class Channel<T> {
  private streamOut = new ReadableStreamOut<T>();
  private controller = this.streamOut.controller;

  get stream() {
    return this.streamOut.stream;
  }

  private _isClosedForSend = false;
  get isClosedForSend() {
    return this._isClosedForSend;
  }

  send(value: T) {
    if (this._isClosedForSend) {
      console.error("Channel send is close!!");
      return;
    }
    this.controller.enqueue(value);
  }

  close(cause?: string) {
    console.debug("channel_close", cause);
    this._isClosedForSend = true;
    this.controller.close();
  }

  [Symbol.asyncIterator](): AsyncIterator<T> {
    return streamRead(this.streamOut.stream);
  }
}
