import { cacheGetter } from "../../helper/cacheGetter.ts";
import { createSignal } from "../../helper/createSignal.ts";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import type { $IpcMicroModuleInfo } from "../../helper/types.ts";
import { MicroModule } from "../micro-module.ts";
import {
  $IpcMessage,
  $OnIpcEventMessage,
  $OnIpcRequestMessage,
  $OnIpcStreamMessage,
  IPC_MESSAGE_TYPE,
  type $OnIpcMessage,
} from "./const.ts";
import type { IpcHeaders } from "./IpcHeaders.ts";
import { IpcRequest } from "./IpcRequest.ts";
import type { IpcResponse } from "./IpcResponse.ts";

let ipc_uid_acc = 0;
export abstract class Ipc {
  readonly uid = ipc_uid_acc++;

  /**
   * 是否支持使用 MessagePack 直接传输二进制
   * 在一些特殊的场景下支持字符串传输，比如与webview的通讯
   * 二进制传输在网络相关的服务里被支持，里效率会更高，但前提是对方有 MessagePack 的编解码能力
   * 否则 JSON 是通用的传输协议
   */
  get support_message_pack() {
    return this._support_message_pack;
  }
  protected _support_message_pack = false;
  /**
   * 是否支持使用 Protobuf 直接传输二进制
   * 在网络环境里，protobuf 是更加高效的协议
   */
  get support_protobuf() {
    return this._support_protobuf;
  }
  protected _support_protobuf = false;

  /**
   * 是否支持结构化内存协议传输：
   * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
   */
  get support_raw() {
    return this._support_raw;
  }
  protected _support_raw = false;
  /**
   * 是否支持二进制传输
   */
  get support_binary() {
    return (
      this._support_binary ??
      (this.support_message_pack || this.support_protobuf || this.support_raw)
    );
  }

  protected _support_binary = false;

  abstract readonly remote: $IpcMicroModuleInfo;
  asRemoteInstance() {
    if (this.remote instanceof MicroModule) {
      return this.remote;
    }
  }
  abstract readonly role: string;

  protected _messageSignal = createSignal<$OnIpcMessage>(false);
  postMessage(message: $IpcMessage): void {
    if (this._closed) {
      return;
    }
    this._doPostMessage(message);
  }
  abstract _doPostMessage(data: $IpcMessage): void;
  onMessage = this._messageSignal.listen;

  @cacheGetter()
  private get _onRequestSignal() {
    const signal = createSignal<$OnIpcRequestMessage>(false);
    this.onMessage((request, ipc) => {
      if (request.type === IPC_MESSAGE_TYPE.REQUEST) {
        signal.emit(request, ipc);
      }
    });
    return signal;
  }

  onRequest(cb: $OnIpcRequestMessage) {
    return this._onRequestSignal.listen(cb);
  }

  @cacheGetter()
  private get _onStreamSignal() {
    const signal = createSignal<$OnIpcStreamMessage>(false);
    this.onMessage((request, ipc) => {
      if ("stream_id" in request) {
        signal.emit(request, ipc);
      }
    });
    return signal;
  }
  onStream(cb: $OnIpcStreamMessage) {
    return this._onStreamSignal.listen(cb);
  }

  @cacheGetter()
  private get _onEventSignal() {
    const signal = createSignal<$OnIpcEventMessage>(false);
    this.onMessage((event, ipc) => {
      if (event.type === IPC_MESSAGE_TYPE.EVENT) {
        signal.emit(event, ipc);
      }
    });
    return signal;
  }

  onEvent(cb: $OnIpcEventMessage) {
    return this._onEventSignal.listen(cb);
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
    this._closeSignal.clear();
  }
  private _closeSignal = createSignal<() => unknown>(false);
  onClose = this._closeSignal.listen;

  private _req_id_acc = 0;
  allocReqId(_url?: string) {
    return this._req_id_acc++;
  }

  @cacheGetter()
  private get _reqresMap() {
    const reqresMap = new Map<number, PromiseOut<IpcResponse>>();
    this.onMessage((message) => {
      if (message.type === IPC_MESSAGE_TYPE.RESPONSE) {
        const response_po = reqresMap.get(message.req_id);
        if (response_po) {
          reqresMap.delete(message.req_id);
          response_po.resolve(message);
        } else {
          throw new Error(`no found response by req_id: ${message.req_id}`);
        }
      }
    });
    return reqresMap;
  }

  /** 发起请求并等待响应 */
  request(
    url: string,
    init?: {
      method?: string;
      body?: /* json+text */
      | string
        /* base64 */
        | Uint8Array
        /* stream+base64 */
        | ReadableStream<Uint8Array>;
      headers?: IpcHeaders | HeadersInit;
    }
  ) {
    const req_id = this.allocReqId();
    const ipcRequest = IpcRequest.fromRequest(req_id, this, url, init);
    const result = this.registerReqId(req_id);
    this.postMessage(ipcRequest);
    return result.promise;
  }
  /** 自定义注册 请求与响应 的id */
  registerReqId(req_id = this.allocReqId()) {
    const response_po = new PromiseOut<IpcResponse>();
    this._reqresMap.set(req_id, response_po);
    return response_po;
  }
}
