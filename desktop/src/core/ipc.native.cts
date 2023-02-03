import { createSingle } from "./helper.cjs";
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
} from "./ipc.cjs";
import type { $MicroModule } from "./types.cjs";

type $JSON<T> = {
  [key in keyof T]: T[key] extends Function ? never : T[key];
};

export const $messageToIpcMessage = (
  data: $JSON<$IpcMessage> | "close",
  ipc: Ipc
) => {
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

export class NativeIpc extends Ipc {
  constructor(
    readonly port: MessagePort,
    readonly module: $MicroModule,
    readonly role: IPC_ROLE
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
      this._onMessage.emit(message, this);
    });
    port.start();
  }
  postMessage(message: IpcRequest | IpcResponse): void {
    if (this._closed) {
      return;
    }
    this.port.postMessage(message);
  }
  private _onMessage = createSingle<$IpcOnMessage>();
  onMessage(cb: $IpcOnMessage) {
    return this._onMessage.bind(cb);
  }
  private _closed = false;
  close() {
    if (this._closed) {
      return;
    }
    this._closed = true;
    this.port.postMessage("close");
    this.port.close();
    this._onClose.emit();
  }
  private _onClose = createSingle<() => unknown>();
  onClose(cb: () => unknown) {
    return this._onClose.bind(cb);
  }
}
