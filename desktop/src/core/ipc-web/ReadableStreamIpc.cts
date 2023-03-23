import { encode } from "@msgpack/msgpack";
import once from "lodash/once";
import { u8aConcat } from "../../helper/binaryHelper.cjs";
import { simpleDecoder, simpleEncoder } from "../../helper/encoding.cjs";
import {
  binaryStreamRead,
  ReadableStreamOut,
} from "../../helper/readableStreamHelper.cjs";
import type {
  $IpcMicroModuleInfo,
  $IpcSupportProtocols,
  $PromiseMaybe,
} from "../../helper/types.cjs";
import type { $IpcMessage, IpcMessage, IPC_ROLE } from "../ipc/const.cjs";
import { Ipc } from "../ipc/ipc.cjs";
import { IpcRequest } from "../ipc/IpcRequest.cjs";
import { IpcResponse } from "../ipc/IpcResponse.cjs";
import { $messagePackToIpcMessage } from "./$messagePackToIpcMessage.cjs";
import { $jsonToIpcMessage } from "./$messageToIpcMessage.cjs";

/**
 * 基于 WebReadableStream 的IPC
 *
 * 它会默认构建出一个输出流，
 * 以及需要手动绑定输入流 {@link bindIncomeStream}
 */
export class ReadableStreamIpc extends Ipc {
  constructor(
    readonly remote: $IpcMicroModuleInfo,
    readonly role: IPC_ROLE,
    readonly self_support_protocols: $IpcSupportProtocols = {
      raw: false,
      message_pack: true,
      protobuf: false,
    }
  ) {
    super();
    /** JS 环境里支持 message_pack 协议 */
    this._support_message_pack =
      self_support_protocols.message_pack &&
      remote.ipc_support_protocols.message_pack;
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
    const pong = simpleEncoder("pong", "utf8");
    this._len[0] = pong.length;
    return u8aConcat([this._len_u8a, pong]);
  });

  private _incomne_stream?: ReadableStream<Uint8Array>;
  /**
   * 输入流要额外绑定
   * 注意，非必要不要 await 这个promise
   */
  async bindIncomeStream(stream: $PromiseMaybe<ReadableStream<Uint8Array>>) {
    if (this._incomne_stream !== undefined) {
      throw new Error("in come stream alreay binded.");
    }
    this._incomne_stream = await stream;
    const reader = binaryStreamRead(this._incomne_stream);
    while ((await reader.available()) > 0) {
      const size = await reader.readInt();
      const data = await reader.readBinary(size);

      /// 开始处理数据并做响应
      const message = this.support_message_pack
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
  }

  private _len = new Uint32Array(1);
  private _len_u8a = new Uint8Array(this._len.buffer);
  _doPostMessage(message: $IpcMessage): void {
    var message_raw: IpcMessage<any>;
    if (message instanceof IpcRequest) {
      message_raw = message.ipcReqMessage();
    } else if (message instanceof IpcResponse) {
      message_raw = message.ipcResMessage();
    } else {
      message_raw = message;
    }

    const message_data = this.support_message_pack
      ? encode(message_raw)
      : simpleEncoder(JSON.stringify(message_raw), "utf8");
    this._len[0] = message_data.length;
    const chunk = u8aConcat([this._len_u8a, message_data]);
    this.controller.enqueue(chunk);
  }

  _doClose() {
    this.controller.close();
  }
}
