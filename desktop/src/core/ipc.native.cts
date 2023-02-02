import {
  $IpcMessage,
  Ipc,
  IpcRequest,
  IpcResponse,
  IpcStream,
  IpcStreamEnd,
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
      data.body,
      data.headers
    );
  } else if (data.type === IPC_DATA_TYPE.RESPONSE) {
    message = new IpcResponse(
      data.req_id,
      data.statusCode,
      data.rawBody,
      data.headers,
      ipc
    );
  } else if (data.type === IPC_DATA_TYPE.STREAM) {
    message = new IpcStream(data.stream_id, data.data);
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
      if (
        message.type === IPC_DATA_TYPE.STREAM ||
        message.type === IPC_DATA_TYPE.STREAM_END
      ) {
        return;
      }
      /// ipc-message
      for (const cb of this._cbs) {
        cb(message, this);
      }
    });
    port.start();
  }
  postMessage(message: IpcRequest | IpcResponse): void {
    if (this._closed) {
      return;
    }
    this.port.postMessage(message);
  }
  private _cbs = new Set<$OnMessage>();
  onMessage(cb: $OnMessage) {
    this._cbs.add(cb);
    return () => this._cbs.delete(cb);
  }
  private _closed = false;
  close() {
    if (this._closed) {
      return;
    }
    this._closed = true;
    this.port.postMessage("close");
    this.port.close();
    for (const cb of this._onclose_cbs) {
      cb();
    }
  }
  private _onclose_cbs = new Set<() => unknown>();
  onClose(cb: () => unknown) {
    this._onclose_cbs.add(cb);
    return () => this._onclose_cbs.delete(cb);
  }
}
export type $OnMessage = (
  message: IpcRequest | IpcResponse,
  ipc: Ipc
) => unknown;
