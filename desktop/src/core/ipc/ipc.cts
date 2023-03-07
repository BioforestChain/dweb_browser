import { cacheGetter } from "../../helper/cacheGetter.cjs";
import { createSignal } from "../../helper/createSignal.cjs";
import { PromiseOut } from "../../helper/PromiseOut.cjs";
import type { $MicroModule } from "../../helper/types.cjs";
import {
  $IpcMessage,
  $OnIpcEventMessage,
  $OnIpcRequestMessage,
  IPC_MESSAGE_TYPE,
  type $OnIpcMessage,
} from "./const.cjs";
import type { IpcHeaders } from "./IpcHeaders.cjs";
import { IpcRequest } from "./IpcRequest.cjs";
import type { IpcResponse } from "./IpcResponse.cjs";

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

  abstract readonly remote: $MicroModule;
  abstract readonly role: string;

  protected _messageSignal = createSignal<$OnIpcMessage>();
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
    const signal = createSignal<$OnIpcRequestMessage>();
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
  private get _onEventSignal() {
    const signal = createSignal<$OnIpcEventMessage>();
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
  private _closeSignal = createSignal<() => unknown>();
  onClose = this._closeSignal.listen;

  private readonly _reqresMap = new Map<number, PromiseOut<IpcResponse>>();
  private _req_id_acc = 0;
  allocReqId(url?: string) {
    return this._req_id_acc++;
  }

  private _inited_req_res = false;
  private _initReqRes() {
    if (this._inited_req_res) {
      return;
    }
    this._inited_req_res = true;
    this.onMessage((message) => {
      if (message.type === IPC_MESSAGE_TYPE.RESPONSE) {
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
  // 先找到错误的位置
  // 需要确定两个问题
  // 是否是应为报错导致无法响应后面的请求
  // 如果是是否可以避免报错？？

  /** 发起请求并等待响应 */
  // 会提供给 http-server模块的 gateway.listener.hookHttpRequest
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
