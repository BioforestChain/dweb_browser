import { encode } from "cbor-x";

import { IpcRequest } from "../ipc/IpcRequest.ts";
import { IpcResponse } from "../ipc/IpcResponse.ts";
import { $IpcMessage, IPC_ROLE, IpcMessage } from "../ipc/const.ts";
import { Ipc } from "../ipc/ipc.ts";
import type { $IpcMicroModuleInfo, $IpcSupportProtocols } from "../types.ts";
import { $messagePackToIpcMessage } from "./$messagePackToIpcMessage.ts";
import { $jsonToIpcMessage, $messageToIpcMessage } from "./$messageToIpcMessage.ts";

export class MessagePortIpc extends Ipc {
  constructor(
    readonly port: MessagePort,
    readonly remote: $IpcMicroModuleInfo,
    readonly role: IPC_ROLE = IPC_ROLE.CLIENT,
    readonly self_support_protocols: $IpcSupportProtocols = {
      raw: true,
      cbor: true,
      protobuf: false,
    }
  ) {
    super();

    /** messageport内置JS对象解码，但也要看对方是否支持接受，比如Android层就只能接受String类型的数据 */
    this._support_raw = self_support_protocols.raw && this.remote.ipc_support_protocols.raw;
    /** JS 环境里支持 cbor 协议，但也要看对等方是否支持 */
    this._support_cbor = self_support_protocols.cbor && this.remote.ipc_support_protocols.cbor;

    port.addEventListener("message", (event) => {
      const message = this.support_raw
        ? $messageToIpcMessage(event.data, this)
        : this.support_cbor
        ? $messagePackToIpcMessage(event.data, this)
        : $jsonToIpcMessage(event.data, this);
      if (message === undefined) {
        console.error("MessagePortIpc.cts unkonwn message", event.data);
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
        this.port.postMessage("pong");
        return;
      }

      this._messageSignal.emit(message, this);
    });
    port.start();
  }

  _doPostMessage(message: $IpcMessage): void {
    // deno-lint-ignore no-explicit-any
    let message_data: any;
    // deno-lint-ignore no-explicit-any
    let message_raw: IpcMessage<any>;
    if (message instanceof IpcRequest) {
      message_raw = message.ipcReqMessage();
    } else if (message instanceof IpcResponse) {
      message_raw = message.ipcResMessage();
    } else {
      message_raw = message;
    }

    if (this.support_raw) {
      message_data = message_raw;
    } else if (this.support_cbor) {
      message_data = encode(message_raw);
    } else {
      message_data = JSON.stringify(message_raw);
    }

    this.port.postMessage(message_data);
  }

  _doClose() {
    this.port.postMessage("close");
    this.port.close();
  }
}
