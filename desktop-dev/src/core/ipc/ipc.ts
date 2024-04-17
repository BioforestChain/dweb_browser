import { PromiseOut } from "../../helper/PromiseOut.ts";
import { CacheGetter } from "../../helper/cacheGetter.ts";
import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { MicroModule } from "../micro-module.ts";
import type { $MicroModuleManifest } from "../types.ts";
import type { IpcHeaders } from "./helper/IpcHeaders.ts";

import { IpcClientRequest } from "./ipc-message/IpcRequest.ts";
import type { IpcResponse } from "./ipc-message/IpcResponse.ts";

import { once } from "../../helper/$once.ts";
import { StateSignal } from "../../helper/StateSignal.ts";
import { mapHelper } from "../../helper/mapHelper.ts";
import { Producer } from "../helper/Producer.ts";
import { $OnFetch, createFetchHandler } from "../helper/ipcFetchHelper.ts";
import { $IpcMessage, IpcServerRequest } from "../index.ts";
import { IpcPool } from "./IpcPool.ts";
import { endpointIpcMessage } from "./endpoint/EndpointIpcMessage.ts";
import { IpcEndpoint } from "./endpoint/IpcEndpoint.ts";
import { PureChannel, pureChannelToIpcEvent } from "./helper/PureChannel.ts";
import { ipcFork } from "./ipc-message/IpcFork.ts";
import {
  $IpcLifecycle,
  ipcLifecycle,
  ipcLifecycleInit,
  ipcLifecycleOpened,
  ipcLifecycleOpening,
} from "./ipc-message/IpcLifecycle.ts";
import { IPC_LIFECYCLE_STATE } from "./ipc-message/internal/IpcLifecycle.ts";
import { IPC_MESSAGE_TYPE } from "./ipc-message/internal/IpcMessage.ts";
export {
  FetchError,
  FetchEvent,
  type $FetchResponse,
  type $OnFetch,
  type $OnFetchReturn,
} from "../helper/ipcFetchHelper.ts";

export class Ipc {
  constructor(
    readonly pid: number,
    readonly endpoint: IpcEndpoint,
    readonly locale: $MicroModuleManifest,
    readonly remote: $MicroModuleManifest,
    readonly pool: IpcPool,
    readonly debugId = `${endpoint.debugId}/${pid}`
  ) {}

  // reqId计数
  #reqIdAcc = 0;
  // 消息生产者，所有的消息在这里分发出去
  #messageProducer = this.endpoint.getIpcMessageProducer(this.pid);

  onMessage(name: string) {
    return this.#messageProducer.producer.consumer(name);
  }

  //#region 生命周期相关的
  #lifecycleLocaleFlow = new StateSignal<$IpcLifecycle>(
    ipcLifecycle(ipcLifecycleInit(this.pid, this.locale, this.remote))
  );

  readonly lifecycleLocaleFlow = this.#lifecycleLocaleFlow.asReadyonly();

  #lifecycleRemoteFlow = this.onMessage(`ipc-lifecycle-remote#${this.pid}`).mapNotNull((message) => {
    if (message.type === IPC_MESSAGE_TYPE.LIFECYCLE) {
      return message;
    }
  });

  readonly lifecycleRemoteFlow = this.#lifecycleRemoteFlow;

  get lifecycle() {
    return this.lifecycleLocaleFlow.state;
  }
  onLifecycle = this.lifecycleLocaleFlow.listen;

  // 标记ipc通道是否激活
  get isActivity() {
    return this.endpoint.isActivity;
  }

  /**等待启动 */
  async awaitOpen(reason?: string) {
    if (this.lifecycle.state.name === IPC_LIFECYCLE_STATE.OPENED) {
      return this.lifecycle;
    }
    const op = new PromiseOut<$IpcLifecycle>();
    const off = this.onLifecycle((lifecycle) => {
      switch (lifecycle.state.name) {
        case IPC_LIFECYCLE_STATE.OPENED: {
          op.resolve(lifecycle);
          break;
        }
        case (IPC_LIFECYCLE_STATE.CLOSED, IPC_LIFECYCLE_STATE.CLOSING): {
          op.reject("endpoint already closed");
          break;
        }
      }
    });
    const lifecycle = await op.promise;
    console.log("awaitOpen", lifecycle, reason);
    off();
    return lifecycle;
  }

  /**
   * 启动，会至少等到endpoint握手完成
   */
  async start(isAwait = true, reason?: string) {
    console.log("ipc-start", reason);
    if (isAwait) {
      this.endpoint.start(true);
      this.startOnce();
      await this.awaitOpen(`from-start ${reason}`);
    } else {
      this.endpoint.start(true);
      this.startOnce();
    }
  }

  startOnce = once(() => {
    console.log("ipc-startOnce", this.lifecycle);
    // 当前状态必须是从init开始
    if (this.lifecycle.state.name === IPC_LIFECYCLE_STATE.INIT) {
      // 告知对方我启动了
      const opening = ipcLifecycle(ipcLifecycleOpening());
      this.#sendLifecycleToRemote(opening);
      this.#lifecycleLocaleFlow.emit(opening);
    } else {
      throw new Error(`fail to start: ipc=${this} state=${this.lifecycle}`);
    }
    // 监听远端生命周期指令，进行协议协商
    this.#lifecycleRemoteFlow((lifecycleRemote) => {
      console.log("ipc-lifecycle-in", `remote=${lifecycleRemote},local=${this.lifecycle}`);
      // 告知启动完成
      const doIpcOpened = () => {
        const opend = ipcLifecycle(ipcLifecycleOpened());
        this.#sendLifecycleToRemote(opend);
        this.#lifecycleLocaleFlow.emit(opend);
      };
      // 处理远端生命周期
      switch (lifecycleRemote.state.name) {
        case (IPC_LIFECYCLE_STATE.CLOSING, IPC_LIFECYCLE_STATE.CLOSED): {
          this.close();
          break;
        }
        // 收到 opened 了，自己也设置成 opened，代表正式握手成功
        case IPC_LIFECYCLE_STATE.OPENED: {
          if (this.lifecycle.state.name === IPC_LIFECYCLE_STATE.OPENING) {
            doIpcOpened();
          }
          break;
        }
        // 如果对方是 init，代表刚刚初始化，那么发送目前自己的状态
        case IPC_LIFECYCLE_STATE.INIT: {
          this.#sendLifecycleToRemote(this.lifecycle);
          break;
        }
        // 等收到对方 Opening ，说明对方也开启了，那么开始协商协议，直到一致后才进入 Opened
        case IPC_LIFECYCLE_STATE.OPENING: {
          doIpcOpened();
          break;
        }
      }
    });
    // 监听并分发 所有的消息
    this.onMessage(`fork#${this.debugId}`).collect((event) => {
      const ipcFork = event.consumeMapNotNull((data) => {
        if (data.type === IPC_MESSAGE_TYPE.FORK) {
          return data;
        }
      });
      if (ipcFork === undefined) {
        return;
      }
      const forkedIpc = new Ipc(ipcFork.pid, this.endpoint, this.locale, this.remote, this.pool);
      this.pool.safeCreatedIpc(forkedIpc, ipcFork.autoStart, ipcFork.startReason);
      mapHelper.getOrPut(this.forkedIpcMap, forkedIpc.pid, () => new PromiseOut()).resolve(forkedIpc);
      this.#forkProducer.send(forkedIpc);
    });
  });
  //#region fork

  private forkedIpcMap = new Map<number, PromiseOut<Ipc>>();
  waitForkedIpc(pid: number) {
    return mapHelper.getOrPut(this.forkedIpcMap, pid, () => new PromiseOut()).promise;
  }

  /**
   * 在现有的线路中分叉出一个ipc通道
   * 如果自定义了 locale/remote，那么说明自己是帮别人代理
   */
  async fork(
    locale: $MicroModuleManifest = this.locale,
    remote: $MicroModuleManifest = this.remote,
    autoStart: boolean = false,
    startReason?: string
  ) {
    await this.awaitOpen("then-fork");
    const forkedIpc = this.pool.createIpc(
      this.endpoint,
      this.endpoint.generatePid(),
      locale,
      remote,
      autoStart,
      startReason
    );
    mapHelper.getOrPut(this.forkedIpcMap, forkedIpc.pid, () => new PromiseOut()).resolve(forkedIpc);
    // 自触发
    this.#forkProducer.send(forkedIpc);
    // 通知对方
    postMessage(
      ipcFork(
        forkedIpc.pid,
        autoStart,
        /// 对调locale/remote
        forkedIpc.remote,
        forkedIpc.locale,
        startReason
      )
    );
    return forkedIpc;
  }

  #forkProducer = new Producer(`fork#${this.debugId}`);
  onFork(name: string) {
    return this.#forkProducer.consumer(name);
  }
  //#endregion

  //#region 消息相关的
  #messagePipeMap<R>(name: string, mapNotNull: (value: $IpcMessage) => R | undefined) {
    const producer = new Producer<R>(this.#messageProducer.producer.name + "/" + name);
    this.onClosed((reason) => {
      return producer.close(reason);
    });
    const consumer = this.onMessage(name);
    consumer.mapNotNull(mapNotNull)((event) => {
      producer.emit(event);
    });
    producer.onClose(() => {
      consumer.close();
    });
    return producer;
  }
  accessor #requestProducer = new CacheGetter(() =>
    this.#messagePipeMap("request", (ipcMessage) => {
      if (ipcMessage instanceof IpcClientRequest) {
        return ipcMessage.toServer(this);
      } else if (ipcMessage instanceof IpcServerRequest) {
        return ipcMessage;
      }
    })
  );
  onRequest(name: string) {
    return this.#requestProducer.value.consumer(name);
  }

  //#endregion

  /**
   * 向远端发送 生命周期 信号
   */
  #sendLifecycleToRemote(state: $IpcLifecycle) {
    console.log("lifecycle-out", state);
    this.endpoint.postIpcMessage(endpointIpcMessage(this.pid, state));
  }

  protected _closeSignal = createSignal<(reason?: string) => unknown>(false);
  onClosed = this._closeSignal.listen;
  asRemoteInstance() {
    if (this.remote instanceof MicroModule) {
      return this.remote;
    }
  }

  // deno-lint-ignore no-explicit-any
  private _createSignal<T extends $Callback<any[]>>(autoStart?: boolean) {
    const signal = createSignal<T>(autoStart);
    this.onClosed(() => signal.clear());
    return signal;
  }

  private _messageSignal = this._createSignal<$OnIpcMessage>(false);

  _doPostMessage(pid: number, data: $IpcMessage): void;

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
    const ipcRequest = IpcClientRequest.fromRequest(reqId, this, url, init);
    return ipcRequest;
  }

  /** 发起请求并等待响应 */
  request(url: IpcClientRequest): Promise<IpcResponse>;
  request(url: string, init?: $IpcRequestInit): Promise<IpcResponse>;
  request(input: string | IpcClientRequest, init?: $IpcRequestInit) {
    const ipcRequest = input instanceof IpcClientRequest ? input : this._buildIpcRequest(input, init);
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

  // 标记是否启动完成
  startDeferred = new PromiseOut<IpcLifeCycle>();
  awaitStart = this.startDeferred.promise;

  //#endregion
  //#region close start

  private get isClosed() {
    return this.ipcLifecycleState == ENDPOINT_LIFECYCLE_STATE.CLOSED;
  }

  // 告知对面我要关闭了
  async tryClose() {
    // 开始关闭
    this.ipcLifecycleState = ENDPOINT_LIFECYCLE_STATE.CLOSING;
    await this.postMessage(IpcLifeCycle.closing());
  }

  /**关闭ipc */
  private _isClose = false;
  async close() {
    if (this._isClose) {
      return;
    }
    this._isClose = true;
    console.log("🌼ipc close worker", this.channelId);
    if (!this.isClosed) {
      await this.tryClose();
    }
    this.destroy();
  }

  /**销毁ipc */
  private _isDestroy = false;
  private async destroy() {
    if (this._isDestroy) {
      return;
    }
    this._isDestroy = true;
    console.log("🌼ipc destroy worker", this.channelId);
    // 我彻底关闭了
    await this.postMessage(IpcLifeCycle.close());
    this._closeSignal.emitAndClear();
    this._doClose();
    this.ipcLifecycleState = ENDPOINT_LIFECYCLE_STATE.CLOSED;
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
