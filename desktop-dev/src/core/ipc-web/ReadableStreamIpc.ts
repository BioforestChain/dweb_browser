import { encode } from "cbor-x";
import { IPC_MESSAGE_TYPE } from "../../core/ipc/const.ts";
import { once } from "../../helper/$once.ts";
import { u8aConcat } from "../../helper/binaryHelper.ts";
import { simpleDecoder, simpleEncoder } from "../../helper/encoding.ts";
import { ReadableStreamOut, binaryStreamRead } from "../../helper/stream/readableStreamHelper.ts";
import { $PromiseMaybe } from "../helper/types.ts";
import type { $IpcMessage, IPC_ROLE, IpcMessage } from "../ipc/const.ts";
import { Ipc } from "../ipc/ipc.ts";
import type { $IpcSupportProtocols, $MicroModuleManifest } from "../types.ts";
import { $messagePackToIpcMessage } from "./$messagePackToIpcMessage.ts";
import { $jsonToIpcMessage } from "./$messageToIpcMessage.ts";

/**
 * 基于 WebReadableStream 的IPC
 *
 * 它会默认构建出一个输出流，
 * 以及需要手动绑定输入流 {@link bindIncomeStream}
 */
export class ReadableStreamIpc extends Ipc {
  constructor(
    readonly remote: $MicroModuleManifest,
    readonly role: IPC_ROLE,
    readonly self_support_protocols: $IpcSupportProtocols = {
      raw: false,
      cbor: true,
      protobuf: false,
    }
  ) {
    super();
    /** JS 环境里支持 cbor 协议 */
    this._support_cbor = self_support_protocols.cbor && remote.ipc_support_protocols.cbor;
  }
  #rso = new ReadableStreamOut<Uint8Array>();
  /** 这是输出流，给外部读取用的 */
  get stream() {
    return this.#rso.stream;
  }
  get controller() {
    return this.#rso.controller;
  }

  private PONG_DATA = once(() => {
    const pong = encode("pong");
    return ReadableStreamIpc.concatLen(pong);
  });

  private CLOSE_DATA = once(() => {
    const close = encode("close");
    return ReadableStreamIpc.concatLen(close);
  });

  private _incomne_stream?: ReadableStream<Uint8Array>;
  /**
   * 输入流要额外绑定
   * 注意，非必要不要 await 这个promise
   */
  async bindIncomeStream(stream: $PromiseMaybe<ReadableStream<Uint8Array>>, options: { signal?: AbortSignal } = {}) {
    if (this._incomne_stream !== undefined) {
      throw new Error("in come stream alreay binded.");
    }
    this._incomne_stream = await stream;
    const { signal } = options;
    const reader = binaryStreamRead(this._incomne_stream, { signal });
    this.onClose(() => {
      reader.throw("output stream closed");
    });
    while ((await reader.available()) > 0) {
      const size = await reader.readInt();
      const data = await reader.readBinary(size);

      /// 开始处理数据并做响应
      const message = this.support_cbor
        ? $messagePackToIpcMessage(data, this)
        : $jsonToIpcMessage(simpleDecoder(data, "utf8"), this);
      if (message === undefined) {
        console.error("unkonwn message", data);
        return;
      }

      if (message === "pong") {
        return;
      }

      if (message === "close") {
        this.close();
        return;
      }
      if (message === "ping") {
        this.controller.enqueue(this.PONG_DATA());
        return;
      }
      this._messageSignal.emit(message, this);
    }
    /// 输入流结束，输出流也要一并关闭
    this.close();
  }

  private static _len = new Uint32Array(1);
  private static _len_u8a = new Uint8Array(this._len.buffer);
  static concatLen = (data: Uint8Array) => {
    this._len[0] = data.length;
    return u8aConcat([this._len_u8a, data]);
  };
  _doPostMessage(message: $IpcMessage): void {
    // deno-lint-ignore no-explicit-any
    let message_raw: IpcMessage<any>;
    // 使用 type 判断
    if (message.type === IPC_MESSAGE_TYPE.REQUEST) {
      message_raw = message.ipcReqMessage();
    } else if (message.type === IPC_MESSAGE_TYPE.RESPONSE) {
      message_raw = message.ipcResMessage();
    } else {
      message_raw = message;
    }

    const message_data = this.support_cbor ? encode(message_raw) : simpleEncoder(JSON.stringify(message_raw), "utf8");
    const chunk = ReadableStreamIpc.concatLen(message_data);
    this.controller.enqueue(chunk);
  }

  _doClose() {
    this.controller.enqueue(this.CLOSE_DATA());
    this.controller.close();
  }
}
