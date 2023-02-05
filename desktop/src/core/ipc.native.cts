import { decode, encode } from "@msgpack/msgpack";
import { createSignal } from "../helper/createSignal.cjs";
import type { $MicroModule } from "../helper/types.cjs";
import {
  $IpcMessage,
  $IpcOnMessage,
  Ipc,
  IpcRequest,
  IpcResponse,
  IpcStreamData,
  IpcStreamEnd,
  IpcStreamPull,
  IPC_DATA_TYPE,
  IPC_ROLE,
} from "./ipc/index.cjs";

type $JSON<T> = {
  [key in keyof T]: T[key] extends Function ? never : T[key];
};

export const $messageToIpcMessage = (
  data: $JSON<$IpcMessage> | "close" | Uint8Array,
  ipc: Ipc
) => {
  if (data instanceof Uint8Array) {
    data = decode(data) as $JSON<$IpcMessage>;
  }
  let message: undefined | $IpcMessage | "close";

  if (data === "close") {
    message = data;
  } else if (data.type === IPC_DATA_TYPE.REQUEST) {
    message = new IpcRequest(
      data.req_id,
      data.method,
      data.url,
      data.rawBody,
      data.headers,
      ipc
    );
  } else if (data.type === IPC_DATA_TYPE.RESPONSE) {
    message = new IpcResponse(
      data.req_id,
      data.statusCode,
      data.rawBody,
      data.headers,
      ipc
    );
  } else if (data.type === IPC_DATA_TYPE.STREAM_DATA) {
    message = new IpcStreamData(data.stream_id, data.data);
  } else if (data.type === IPC_DATA_TYPE.STREAM_PULL) {
    message = new IpcStreamPull(data.stream_id, data.desiredSize);
  } else if (data.type === IPC_DATA_TYPE.STREAM_END) {
    message = new IpcStreamEnd(data.stream_id);
  }
  return message;
};

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

      /// ipc-message
      this._messageSignal.emit(message, this);
    });
    port.start();
  }
  postMessage(message: IpcRequest | IpcResponse): void {
    if (this._closed) {
      return;
    }
    this.port.postMessage(
      this.support_message_pack ? encode(message) : message
    );
  }
  private _messageSignal = createSignal<$IpcOnMessage>();
  onMessage(cb: $IpcOnMessage) {
    return this._messageSignal.bind(cb);
  }
  private _closed = false;
  close() {
    if (this._closed) {
      return;
    }
    this._closed = true;
    this.port.postMessage("close");
    this.port.close();
    this._closeSignal.emit();
  }
  private _closeSignal = createSignal<() => unknown>();
  onClose(cb: () => unknown) {
    return this._closeSignal.bind(cb);
  }
}

export class NativeIpc extends MessagePortIpc {
  constructor(
    port: MessagePort,
    remote: $MicroModule,
    role: IPC_ROLE,
    /// 原生之间的互相传输，默认支持 message-pack 格式
    support_message_pack = true
  ) {
    super(port, remote, role, support_message_pack);
  }
}
