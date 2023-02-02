import { Ipc, IpcRequest, IpcResponse, IPC_DATA_TYPE } from "./ipc.cjs";

type $JSON<T> = {
  [key in keyof T]: T[key] extends Function ? never : T[key];
};

export const $messageToIpcMessage = (
  data: $JSON<IpcRequest | IpcResponse> | "close"
) => {
  let message: IpcRequest | IpcResponse | "close" | undefined;

  /*  if (data instanceof IpcRequest || data instanceof IpcResponse) {
      message = data;
   } else  */ if (data === "close") {
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
      data.body,
      data.headers
    );
  }
  return message;
};

export class NativeIpc extends Ipc {
  constructor(readonly port: MessagePort) {
    super();
    port.addEventListener("message", (event) => {
      const message = $messageToIpcMessage(event.data);

      if (message === undefined) {
        return;
      }
      if (message === "close") {
        this.close();
        return;
      }
      /// ipc-message
      for (const cb of this._cbs) {
        cb(message);
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
  private _cbs = new Set<(message: IpcRequest | IpcResponse) => unknown>();
  onMessage(cb: (message: IpcRequest | IpcResponse) => unknown) {
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
