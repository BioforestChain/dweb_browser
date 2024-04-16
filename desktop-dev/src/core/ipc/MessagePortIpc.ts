import { encode } from "cbor-x";

import { IpcPool, IpcPoolPack, IpcPoolPackString } from "../index.ts";
import type { $IpcSupportProtocols, $MicroModuleManifest } from "../types.ts";
import { IpcRequest } from "./ipc-message/IpcRequest.ts";
import { IpcResponse } from "./ipc-message/IpcResponse.ts";
import {
  $cborToIpcMessage,
  $jsonToIpcMessage,
  $messageToIpcMessage,
  $uint8ArrayToIpcMessage,
} from "./helper/$messageToIpcMessage.ts";
import { $IpcMessage } from "./helper/const.ts";
import { Ipc } from "./ipc.ts";

export class MessagePortIpc extends Ipc {
  constructor(
    readonly port: MessagePort,
    readonly remote: $MicroModuleManifest,
    override channelId: string,
    override endpoint: IpcPool,
    readonly self_support_protocols: $IpcSupportProtocols = {
      raw: true,
      cbor: true,
      protobuf: false,
    }
  ) {
    super(channelId, endpoint);

    /** messageport内置JS对象解码，但也要看对方是否支持接受，比如Android层就只能接受String类型的数据 */
    this._support_raw = self_support_protocols.raw && this.remote.ipc_support_protocols.raw;
    /** JS 环境里支持 cbor 协议，但也要看对等方是否支持 */
    this._support_cbor = self_support_protocols.cbor && this.remote.ipc_support_protocols.cbor;

    port.addEventListener("message", (event) => {
      const message = this.support_raw
        ? $messageToIpcMessage(event.data, this)
        : event.data instanceof Uint8Array
        ? $uint8ArrayToIpcMessage(event.data, this)
        : this.support_cbor
        ? $cborToIpcMessage(event.data, this)
        : $jsonToIpcMessage(event.data, this);
      if (message === undefined) {
        console.error("MessagePortIpc.cts unkonwn message", event.data);
        return;
      }
      this.endpoint.emitMessage(message, this);
    });
    port.start();
  }

  _doPostMessage(pid: number, message: $IpcMessage): void {
    // deno-lint-ignore no-explicit-any
    let message_raw: any;
    if (message instanceof IpcRequest) {
      message_raw = message.ipcReqMessage();
    } else if (message instanceof IpcResponse) {
      message_raw = message.ipcResMessage();
    } else {
      message_raw = message;
    }
    const pack = new IpcPoolPack(pid, message_raw);
    // deno-lint-ignore no-explicit-any
    let message_data: any;
    if (this.support_raw) {
      // 内存直传
      message_data = pack;
    } else if (this.support_cbor) {
      message_data = encode(pack);
    } else {
      message_data = JSON.stringify(new IpcPoolPackString(pid, JSON.stringify(message_raw)));
    }
    this.port.postMessage(message_data);
  }

  _doClose() {
    this.port.close();
  }
}
