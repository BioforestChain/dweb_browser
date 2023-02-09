import { encode } from "@msgpack/msgpack";
import { u8aConcat } from "../../helper/binaryHelper.cjs";
import { simpleEncoder } from "../../helper/encoding.cjs";
import {
  ReadableStreamOut,
  streamRead,
} from "../../helper/readableStreamHelper.cjs";
import type { $MicroModule } from "../../helper/types.cjs";
import type { $IpcMessage, IPC_ROLE } from "../ipc/const.cjs";
import { Ipc } from "../ipc/ipc.cjs";
import { $messageToIpcMessage } from "./$messageToIpcMessage.cjs";

/**
 * 基于 WebReadableStream 的IPC
 *
 * 它会默认构建出一个输出流，
 * 以及需要手动绑定输入流 {@link bindIncomeStream}
 */
export class ReadableStreamIpc extends Ipc {
  constructor(
    readonly remote: $MicroModule,
    readonly role: IPC_ROLE,
    /** MessagePort 默认支持二进制传输 */
    readonly support_message_pack = true
  ) {
    super();
  }
  #rso = new ReadableStreamOut<Uint8Array>();
  /** 这是输出流，给外部读取用的 */
  get stream() {
    return this.#rso.stream;
  }
  get controller() {
    return this.#rso.controller;
  }

  private _incomne_stream?: ReadableStream<Uint8Array>;
  /** 输入流要额外绑定 */
  async bindIncomeStream(stream: ReadableStream<Uint8Array>) {
    if (this._incomne_stream !== undefined) {
      throw new Error("in come stream alreay binded.");
    }
    this._incomne_stream = stream;
    let cache = new Uint8Array(0);
    for await (const chunk of streamRead(stream)) {
      cache = u8aConcat([cache, chunk]);
      const len = new Uint32Array(cache.buffer, 0, 1)[0];
      // 数据不够，继续等待
      if (cache.length - 4 < len) {
        continue;
      }
      // 数据够了，截断出来使用
      const data = cache.slice(4, len + 4);
      cache = cache.slice(len + 4);

      /// 开始处理数据并做响应
      const message = $messageToIpcMessage(data, this);

      if (message === undefined) {
        return;
      }
      if (message === "close") {
        this.close();
        return;
      }

      this._messageSignal.emit(message, this);
    }
  }

  private _len = new Uint32Array(1);
  private _len_u8a = new Uint8Array(this._len.buffer);
  _doPostMessage(message: $IpcMessage): void {
    const data = this.support_message_pack
      ? encode(message)
      : simpleEncoder(JSON.stringify(message), "utf8");
    this._len[0] = data.length;
    const chunk = u8aConcat([this._len_u8a, data]);
    this.controller.enqueue(chunk);
  }

  _doClose() {
    this.controller.close();
  }
}
