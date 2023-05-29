import { encode } from "@msgpack/msgpack";

import type {
  $IpcMicroModuleInfo,
  $IpcSupportProtocols,
} from "../../helper/types.ts";
import { $IpcMessage, IPC_ROLE, IpcMessage } from "../ipc/const.ts";
import { Ipc } from "../ipc/ipc.ts";
import { IpcRequest } from "../ipc/IpcRequest.ts";
import { IpcResponse } from "../ipc/IpcResponse.ts";
import { $messagePackToIpcMessage } from "./$messagePackToIpcMessage.ts";
import {
  $jsonToIpcMessage,
  $messageToIpcMessage,
} from "./$messageToIpcMessage.ts";

export class MessagePortIpc extends Ipc {
  constructor(
    readonly port: MessagePort,
    readonly remote: $IpcMicroModuleInfo,
    readonly role: IPC_ROLE = IPC_ROLE.CLIENT,
    readonly self_support_protocols: $IpcSupportProtocols = {
      raw: true,
      message_pack: true,
      protobuf: false,
    }
  ) {
    super();

    /** messageport内置JS对象解码，但也要看对方是否支持接受，比如Android层就只能接受String类型的数据 */
    this._support_raw =
      self_support_protocols.raw && this.remote.ipc_support_protocols.raw;
    /** JS 环境里支持 message_pack 协议，但也要看对等方是否支持 */
    this._support_message_pack =
      self_support_protocols.message_pack &&
      this.remote.ipc_support_protocols.message_pack;

    port.addEventListener("message", (event) => {
      const message = this.support_raw
        ? $messageToIpcMessage(event.data, this)
        : this.support_message_pack
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
      if ("body" in message && message.body.metaBody.metaId === "rs-31") {
        debugger;
      }
      // console.log("web-message-port-ipc", "onmessage", message);
      this._messageSignal.emit(message, this);
    });
    port.start();
  }

  _doPostMessage(message: $IpcMessage): void {
    let message_data: any;
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
    } else if (this.support_message_pack) {
      message_data = encode(message_raw);
    } else {
      message_data = JSON.stringify(message_raw);
    }

    this.port.postMessage(message_data);
  }

  _doClose() {
    console.log("web-message-port-ipc", "onclose");
    this.port.postMessage("close");
    this.port.close();
  }
}
