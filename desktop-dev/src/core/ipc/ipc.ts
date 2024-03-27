import { PromiseOut } from "../../helper/PromiseOut.ts";
import { CacheGetter } from "../../helper/cacheGetter.ts";
import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { MicroModule } from "../micro-module.ts";
import type { $MicroModuleManifest } from "../types.ts";
import { IpcRequest } from "./IpcRequest.ts";
import type { IpcResponse } from "./IpcResponse.ts";
import type { IpcHeaders } from "./helper/IpcHeaders.ts";
import {
  $IpcMessage,
  $OnIpcErrorMessage,
  $OnIpcEventMessage,
  $OnIpcLifeCycleMessage,
  $OnIpcRequestMessage,
  $OnIpcStreamMessage,
  IPC_MESSAGE_TYPE,
  IPC_STATE,
  type $OnIpcMessage,
} from "./helper/const.ts";

import { mapHelper } from "../../helper/mapHelper.ts";
import { $OnFetch, createFetchHandler } from "../helper/ipcFetchHelper.ts";
import { IpcLifeCycle } from "./IpcLifeCycle.ts";
import { IpcPool } from "./IpcPool.ts";
import { PureChannel, pureChannelToIpcEvent } from "./PureChannel.ts";
export {
  FetchError,
  FetchEvent,
  type $FetchResponse,
  type $OnFetch,
  type $OnFetchReturn
} from "../helper/ipcFetchHelper.ts";

let ipc_uid_acc = 0;
let _order_by_acc = 0;
export abstract class Ipc {
  private pid = 0;
  constructor(readonly channelId: string, readonly endpoint: IpcPool) {
    this.pid = endpoint.generatePid(channelId);
  }

  readonly uid = (ipc_uid_acc++).toString();
  static order_by_acc = _order_by_acc++;

  /**
   * 是否支持使用 cbor 直接传输二进制
   */
  get support_cbor() {
    return this._support_cbor;
  }
  protected _support_cbor = false;
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
    return this._support_binary ?? (this.support_cbor || this.support_protobuf || this.support_raw);
  }

  protected _support_binary = false;
  // 跟ipc绑定的模块
  abstract readonly remote: $MicroModuleManifest;
  // 当前ipc生命周期
  private ipcLifeCycleState: IPC_STATE = IPC_STATE.OPENING;
  protected _closeSignal = createSignal<() => unknown>(false);
  onClose = this._closeSignal.listen;
  asRemoteInstance() {
    if (this.remote instanceof MicroModule) {
      return this.remote;
    }
  }

  // deno-lint-ignore no-explicit-any
  private _createSignal<T extends $Callback<any[]>>(autoStart?: boolean) {
    const signal = createSignal<T>(autoStart);
    this.onClose(() => signal.clear());
    return signal;
  }

  private _messageSignal = this._createSignal<$OnIpcMessage>(false);

  abstract _doPostMessage(pid: number, data: $IpcMessage): void;
  onMessage = this._messageSignal.listen;

  /**分发各类消息到本地*/
  emitMessage = (args: $IpcMessage) => this._messageSignal.emit(args, this);

  private __onRequestSignal = new CacheGetter(() => {
    const signal = this._createSignal<$OnIpcRequestMessage>(false);
    this.onMessage((request, ipc) => {
      if (request.type === IPC_MESSAGE_TYPE.REQUEST) {
        signal.emit(request, ipc);
      }
    });
    return signal;
  });
  private get _onRequestSignal() {
    return this.__onRequestSignal.value;
  }

  onRequest(cb: $OnIpcRequestMessage) {
    return this._onRequestSignal.listen(cb);
  }

  onFetch(...handlers: $OnFetch[]) {
    const onRequest = createFetchHandler(handlers);
    return onRequest.extendsTo(this.onRequest(onRequest));
  }
  private __onStreamSignal = new CacheGetter(() => {
    const signal = this._createSignal<$OnIpcStreamMessage>(false);
    this.onMessage((request, ipc) => {
      if ("stream_id" in request) {
        signal.emit(request, ipc);
      }
    });
    return signal;
  });
  private get _onStreamSignal() {
    return this.__onStreamSignal.value;
  }
  onStream(cb: $OnIpcStreamMessage) {
    return this._onStreamSignal.listen(cb);
  }

  private _onEventSignal = new CacheGetter(() => {
    const signal = this._createSignal<$OnIpcEventMessage>(false);
    this.onMessage((event, ipc) => {
      if (event.type === IPC_MESSAGE_TYPE.EVENT) {
        signal.emit(event, ipc);
      }
    });
    return signal;
  });

  onEvent(cb: $OnIpcEventMessage) {
    return this._onEventSignal.value.listen(cb);
  }
  // lifecycle start
  private _lifeCycleSignal = new CacheGetter(() => {
    const signal = this._createSignal<$OnIpcLifeCycleMessage>(false);
    this.onMessage((event, ipc) => {
      if (event.type === IPC_MESSAGE_TYPE.LIFE_CYCLE) {
        signal.emit(event, ipc);
      }
    });
    return signal;
  });

  onLifeCycle(cb: $OnIpcLifeCycleMessage) {
    return this._lifeCycleSignal.value.listen(cb);
  }
  // lifecycle end

  private _errorSignal = new CacheGetter(() => {
    const signal = this._createSignal<$OnIpcErrorMessage>(false);
    this.onMessage((event, ipc) => {
      if (event.type === IPC_MESSAGE_TYPE.ERROR) {
        signal.emit(event, ipc);
      }
    });
    return signal;
  });

  onError(cb: $OnIpcErrorMessage) {
    return this._errorSignal.value.listen(cb);
  }

  private _reqId_acc = 0;
  allocReqId(_url?: string) {
    return this._reqId_acc++;
  }
  private __reqresMap = new CacheGetter(() => {
    const reqresMap = new Map<number, PromiseOut<IpcResponse>>();
    this.onMessage((message) => {
      if (message.type === IPC_MESSAGE_TYPE.RESPONSE) {
        const response_po = reqresMap.get(message.reqId);
        if (response_po) {
          reqresMap.delete(message.reqId);
          response_po.resolve(message);
        } else {
          throw new Error(`no found response by reqId: ${message.reqId}`);
        }
      }
    });
    return reqresMap;
  });
  private get _reqresMap() {
    return this.__reqresMap.value;
  }

  private _buildIpcRequest(url: string, init?: $IpcRequestInit) {
    const reqId = this.allocReqId();
    const ipcRequest = IpcRequest.fromRequest(reqId, this, url, init);
    return ipcRequest;
  }

  /** 发起请求并等待响应 */
  request(url: IpcRequest): Promise<IpcResponse>;
  request(url: string, init?: $IpcRequestInit): Promise<IpcResponse>;
  request(input: string | IpcRequest, init?: $IpcRequestInit) {
    const ipcRequest = input instanceof IpcRequest ? input : this._buildIpcRequest(input, init);
    const result = this.registerReqId(ipcRequest.reqId);
    this.postMessage(ipcRequest);
    return result.promise;
  }
  /** 自定义注册 请求与响应 的id */
  registerReqId(reqId = this.allocReqId()) {
    return mapHelper.getOrPut(this._reqresMap, reqId, () => new PromiseOut());
  }

  /**
   * 代理管道 发送数据 与 接收数据
   * @param channel
   */
  async pipeToChannel(channelId: string, channel: PureChannel) {
    await pureChannelToIpcEvent(channelId, this, channel, channel.income.controller, channel.outgoing.stream, () =>
      channel.afterStart()
    );
  }
  /**
   * 代理管道 发送数据 与 接收数据
   * @param channel
   */
  async pipeFromChannel(channelId: string, channel: PureChannel) {
    await pureChannelToIpcEvent(channelId, this, channel, channel.outgoing.controller, channel.income.stream, () =>
      channel.start()
    );
  }

  async postMessage(message: $IpcMessage) {
    if (this.isClosed) {
      console.log(`ipc postMessage [${this.channelId}] already closed:discard ${JSON.stringify(message)}`);
      return;
    }
    // 等待通信建立完成
    if (!this.isActivity && !(message instanceof IpcLifeCycle)) {
      await this.awaitStart;
    }
    // 发到pool进行分发消息
    this._doPostMessage(this.pid, message);
  }

  ready() {
    return this.awaitStart;
  }

  // 标记是否启动完成
  startDeferred = new PromiseOut<IpcLifeCycle>();
  get isActivity() {
    return this.startDeferred.is_finished;
  }
  awaitStart = this.startDeferred.promise;
  // 告知对方我启动了
  start() {
    this.ipcLifeCycleState = IPC_STATE.OPEN;
    // 如果是后连接的也需要发个连接消息  这里唯一可能出现消息的丢失就是通道中消息丢失
    this.postMessage(IpcLifeCycle.opening());
  }

  /**ipc激活回调 */
  initlifeCycleHook() {
    // TODO 跟对方通信 协商数据格式
    // console.log(`🌸 xxlife start=>🍃 ${this.remote.mmid} ${this.channelId}`);
    this.onLifeCycle((lifeCycle, ipc) => {
      switch (lifeCycle.state) {
        // 收到打开中的消息，也告知自己已经准备好了
        case IPC_STATE.OPENING: {
          ipc.postMessage(IpcLifeCycle.open());
          ipc.startDeferred.resolve(lifeCycle);
          break;
        }
        // 收到对方完成开始建立连接
        case IPC_STATE.OPEN: {
          // console.log(`🌸 xxlife start=>🍟 ${ipc.remote.mmid} ${ipc.channelId} ${lifeCycle.state}`);
          if (!ipc.startDeferred.is_finished) {
            ipc.startDeferred.resolve(lifeCycle);
          }
          break;
        }
        // 消息通道开始关闭
        case IPC_STATE.CLOSING: {
          //这里可以接受最后一些消息
          this.ipcLifeCycleState = IPC_STATE.CLOSING;
          this.postMessage(IpcLifeCycle.close());
          break;
        }
        // 对方关了，代表没有消息发过来了，我也关闭
        case IPC_STATE.CLOSED: {
          this.destroy();
        }
      }
    });
  }

  /**----- close start*/
  abstract _doClose(): void;

  private get isClosed() {
    return this.ipcLifeCycleState == IPC_STATE.CLOSED;
  }

  // 告知对面我要关闭了
  tryClose = () => {
    // 开始关闭
    this.ipcLifeCycleState = IPC_STATE.CLOSING;
    this.postMessage(IpcLifeCycle.closing());
  };

  private _isClose = false;
  close() {
    if (this._isClose) {
      return;
    }
    console.log("🌼ipc close worker", this.channelId);
    this._isClose = true;
    if (!this.isClosed) {
      this.tryClose();
    }
  }

  async destroy() {
    if (this.isClosed) {
      return;
    }
    console.log("🌼ipc destroy worker", this.channelId);
    // 我彻底关闭了
    await this.postMessage(IpcLifeCycle.close());
    this._closeSignal.emitAndClear();
    this._doClose();
    this.ipcLifeCycleState = IPC_STATE.CLOSED;
  }

  /**----- close end*/
}
export type $IpcRequestInit = {
  method?: string;
  body?: /* json+text */
  | null
    | string
    /* base64 */
    | Uint8Array
    /* stream+base64 */
    | Blob
    | ReadableStream<Uint8Array>;
  headers?: IpcHeaders | HeadersInit;
};
