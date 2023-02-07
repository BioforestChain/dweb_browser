import { encode } from "@msgpack/msgpack";
import type { $MicroModule } from "../../helper/types.cjs";
import type { $IpcMessage, IPC_ROLE } from "../ipc/const.cjs";
import { Ipc } from "../ipc/ipc.cjs";
import { $messageToIpcMessage } from "./$messageToIpcMessage.cjs";

export class MessagePortIpc extends Ipc {
  constructor(
    readonly port: MessagePort,
    readonly remote: $MicroModule,
    readonly role: IPC_ROLE,
    /** MessagePort 默认支持二进制传输 */
    readonly support_message_pack = true
  ) {
    super();
    port.addEventListener("message", (event) => {
      const message = $messageToIpcMessage(event.data, this);

      if (message === undefined) {
        return;
      }
      if (message === "close") {
        this.close();
        return;
      }

      this._messageSignal.emit(message, this);
    });
    port.start();
  }
  _doPostMessage(message: $IpcMessage): void {
    this.port.postMessage(
      this.support_message_pack ? encode(message) : message
    );
  }

  _doClose() {
    this.port.postMessage("close");
    this.port.close();
  }
}
