import { encode } from "@msgpack/msgpack";
import type {
  $IpcMicroModuleInfo,
  $IpcSupportProtocols,
} from "../../helper/types.cjs";
import { $IpcMessage, IpcMessage, IPC_ROLE } from "../ipc/const.cjs";
import { Ipc } from "../ipc/ipc.cjs";
import { IpcRequest } from "../ipc/IpcRequest.cjs";
import { IpcResponse } from "../ipc/IpcResponse.cjs";
import { $jsonToIpcMessage } from "./$jsonToIpcMessage.cjs";
import { $messagePackToIpcMessage } from "./$messagePackToIpcMessage.cjs";
import { $messageToIpcMessage } from "./$messageToIpcMessage.cjs";

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
        console.error("unkonwn message", event.data);
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
    var message_data: any;
    var message_raw: IpcMessage<any>;
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
    this.port.postMessage("close");
    this.port.close();
  }
}
