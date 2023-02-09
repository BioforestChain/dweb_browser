import { isBinary } from "../../helper/binaryHelper.cjs";
import { createSignal } from "../../helper/createSignal.cjs";
import { PromiseOut } from "../../helper/PromiseOut.cjs";
import type { $MicroModule } from "../../helper/types.cjs";
import {
  IPC_DATA_TYPE,
  type $IpcMessage,
  type $OnIpcMessage,
  type IPC_ROLE,
} from "./const.cjs";
import { IpcHeaders } from "./IpcHeaders.cjs";
import { IpcRequest } from "./IpcRequest.cjs";
import type { IpcResponse } from "./IpcResponse.cjs";

let ipc_uid_acc = 0;
export abstract class Ipc {
  /**
   * 是否支持使用 MessagePack 直接传输二进制
   * 在一些特殊的场景下支持字符串传输，比如与webview的通讯
   * 二进制传输在网络相关的服务里被支持，里效率会更高，但前提是对方有 MessagePack 的编解码能力
   * 否则 JSON 是通用的传输协议
   */
  abstract support_message_pack: boolean;
  readonly uid = ipc_uid_acc++;
  abstract readonly remote: $MicroModule;
  abstract readonly role: IPC_ROLE;

  protected _messageSignal = createSignal<$OnIpcMessage>();
  postMessage(message: $IpcMessage): void {
    if (this._closed) {
      return;
    }
    this._doPostMessage(message);
  }
  abstract _doPostMessage(data: $IpcMessage): void;
  onMessage(cb: $OnIpcMessage) {
    return this._messageSignal.listen(cb);
  }

  abstract _doClose(): void;

  private _closed = false;
  close() {
    if (this._closed) {
      return;
    }
    this._closed = true;
    this._doClose();
    this._closeSignal.emit();
  }
  private _closeSignal = createSignal<() => unknown>();
  onClose(cb: () => unknown) {
    return this._closeSignal.listen(cb);
  }

  private readonly _reqresMap = new Map<number, PromiseOut<IpcResponse>>();
  private _req_id_acc = 0;
  allocReqId() {
    return this._req_id_acc++;
  }

  private _inited_req_res = false;
  private _initReqRes() {
    if (this._inited_req_res) {
      return;
    }
    this._inited_req_res = true;
    this.onMessage((message) => {
      if (message.type === IPC_DATA_TYPE.RESPONSE) {
        const response_po = this._reqresMap.get(message.req_id);
        if (response_po) {
          this._reqresMap.delete(message.req_id);
          response_po.resolve(message);
        } else {
          throw new Error(`no found response by req_id: ${message.req_id}`);
        }
      }
    });
  }

  /** 发起请求并等待响应 */
  request(
    url: string,
    init: {
      method?: string;
      body?: /* json+text */
      | string
        /* base64 */
        | Uint8Array
        /* stream+base64 */
        | ReadableStream<Uint8Array>;
      headers?: IpcHeaders | HeadersInit;
    } = {}
  ) {
    const req_id = this.allocReqId();
    const method = init.method ?? "GET";
    const headers =
      init.headers instanceof IpcHeaders
        ? init.headers
        : new IpcHeaders(init.headers);
    let ipcRequest: IpcRequest;
    if (isBinary(init.body)) {
      ipcRequest = IpcRequest.fromBinary(
        init.body,
        req_id,
        method,
        url,
        headers,
        this
      );
    } else if (init.body instanceof ReadableStream) {
      ipcRequest = IpcRequest.fromStream(
        init.body,
        req_id,
        method,
        url,
        headers,
        this
      );
    } else {
      ipcRequest = IpcRequest.fromText(
        init.body ?? "",
        req_id,
        method,
        url,
        headers
      );
    }

    this.postMessage(ipcRequest);
    return this.registerReqId(req_id).promise;
  }
  /** 自定义注册 请求与响应 的id */
  registerReqId(req_id = this.allocReqId()) {
    const response_po = new PromiseOut<IpcResponse>();
    this._reqresMap.set(req_id, response_po);
    this._initReqRes();
    return response_po;
  }
}
