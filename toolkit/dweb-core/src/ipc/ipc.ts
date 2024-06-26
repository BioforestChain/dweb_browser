import { PromiseOut } from "@dweb-browser/helper/PromiseOut.ts";
import { CacheGetter } from "@dweb-browser/helper/cacheGetter.ts";
import type { $MicroModuleManifest } from "../types.ts";
import type { IpcHeaders } from "./helper/IpcHeaders.ts";

import { IpcClientRequest, IpcServerRequest } from "./ipc-message/IpcRequest.ts";
import { IpcResponse } from "./ipc-message/IpcResponse.ts";

import { Producer } from "@dweb-browser/helper/Producer.ts";
import { StateSignal } from "@dweb-browser/helper/StateSignal.ts";
import { $once, once } from "@dweb-browser/helper/decorator/$once.ts";
import { mapHelper } from "@dweb-browser/helper/fun/mapHelper.ts";
import { CUSTOM_INSPECT, logger } from "@dweb-browser/helper/logger.ts";
import { promiseAsSignalListener } from "@dweb-browser/helper/promiseSignal.ts";
import type { IpcPool } from "./IpcPool.ts";
import { endpointIpcMessage } from "./endpoint/EndpointIpcMessage.ts";
import type { IpcEndpoint } from "./endpoint/IpcEndpoint.ts";
import { IpcFork } from "./ipc-message/IpcFork.ts";
import {
  IpcLifecycle,
  IpcLifecycleClosed,
  IpcLifecycleClosing,
  IpcLifecycleInit,
  IpcLifecycleOpened,
  IpcLifecycleOpening,
  type $IpcLifecycle,
} from "./ipc-message/IpcLifecycle.ts";
import type { $IpcMessage } from "./ipc-message/IpcMessage.ts";
import { prepareIpcChannel } from "./ipc-message/channel/IpcChannel.ts";
import { IPC_LIFECYCLE_STATE } from "./ipc-message/internal/IpcLifecycle.ts";
import { IPC_MESSAGE_TYPE } from "./ipc-message/internal/IpcMessage.ts";

export class Ipc {
  readonly console = logger(this);
  constructor(
    readonly pid: number,
    readonly endpoint: IpcEndpoint,
    readonly locale: $MicroModuleManifest,
    readonly remote: $MicroModuleManifest,
    readonly pool: IpcPool,
    readonly debugId = `${endpoint.debugId}/${pid}`
  ) {
    // 构造ipc消息生产者，其所有的消息都是由endpoint分发而来
    this.#messageProducer = this.endpoint.getIpcMessageProducerByIpc(this);
    this.#lifecycleLocaleFlow = new StateSignal<$IpcLifecycle>(
      IpcLifecycle(IpcLifecycleInit(this.pid, this.locale, this.remote)),
      IpcLifecycle.equals
    );
    this.lifecycleLocaleFlow = this.#lifecycleLocaleFlow.asReadyonly();
    this.onLifecycle = this.lifecycleLocaleFlow.listen;
    this.#lifecycleRemoteFlow = this.onMessage(`ipc-lifecycle-remote#${this.pid}`).mapNotNull((message) => {
      if (message.type === IPC_MESSAGE_TYPE.LIFECYCLE) {
        return message;
      }
    });
    this.lifecycleRemoteFlow = this.#lifecycleRemoteFlow;
    this.#forkProducer = new Producer<Ipc>(`fork#${this.debugId}`);

    endpoint.awaitClosed().then((reason) => {
      this.close(reason);
    });
  }
  toString() {
    return `Ipc#${this.debugId}`;
  }
  [CUSTOM_INSPECT]() {
    return this.toString();
  }

  // reqId计数
  #reqIdAcc = 0;
  // 消息生产者，所有的消息在这里分发出去
  #messageProducer;

  onMessage(name: string) {
    return this.#messageProducer.producer.consumer(name);
  }

  //#region 生命周期相关
  #lifecycleLocaleFlow;
  readonly lifecycleLocaleFlow;
  get lifecycle() {
    return this.lifecycleLocaleFlow.state;
  }
  onLifecycle;

  #lifecycleRemoteFlow;
  readonly lifecycleRemoteFlow;
  /**
   * 向远端发送 生命周期 信号
   */
  #sendLifecycleToRemote(state: $IpcLifecycle) {
    this.console.verbose("lifecycle-out", state);
    this.endpoint.postIpcMessage(endpointIpcMessage(this.pid, state));
  }

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
    this.console.verbose("awaitOpen", lifecycle, reason);
    off();
    return lifecycle;
  }

  /**
   * 启动，会至少等到endpoint握手完成
   */
  async start(isAwait = true, reason?: string) {
    this.console.verbose("start", reason);
    if (isAwait) {
      this.endpoint.start(true);
      this.startOnce();
      await this.awaitOpen(`from-start ${reason}`);
    } else {
      this.endpoint.start(true);
      this.startOnce();
    }
  }

  startOnce = $once(() => {
    this.console.verbose("startOnce", this.lifecycle);
    // 当前状态必须是从init开始
    if (this.lifecycle.state.name === IPC_LIFECYCLE_STATE.INIT) {
      // 告知对方我启动了
      const opening = IpcLifecycle(IpcLifecycleOpening());
      this.#sendLifecycleToRemote(opening);
      this.#lifecycleLocaleFlow.emit(opening);
    } else {
      throw new Error(`fail to start: ipc=${this} state=${this.lifecycle}`);
    }
    // 监听远端生命周期指令，进行协议协商
    this.#lifecycleRemoteFlow((lifecycleRemote) => {
      // this.console.verbose("lifecycle-in", `remote=${lifecycleRemote},local=${this.lifecycle}`);
      // 告知启动完成
      const doIpcOpened = () => {
        const opend = IpcLifecycle(IpcLifecycleOpened());
        this.#sendLifecycleToRemote(opend);
        this.#lifecycleLocaleFlow.emit(opend);
      };
      // 处理远端生命周期
      switch (lifecycleRemote.state.name) {
        case (IPC_LIFECYCLE_STATE.CLOSING, IPC_LIFECYCLE_STATE.CLOSED): {
          this.close(lifecycleRemote.state.reason);
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
    this.postMessage(
      IpcFork(
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

  #forkProducer;
  onFork(name: string) {
    return this.#forkProducer.consumer(name);
  }
  //#endregion

  //#region 消息相关的
  #messagePipeMap<R>(name: string, mapNotNull: (value: $IpcMessage) => R | undefined) {
    // 构造一个生产者分发消息
    const producer = new Producer<R>(this.debugId + this.#messageProducer.producer.name + "/" + name);
    this.onClosed((reason) => {
      return producer.close(reason);
    });
    const consumer = this.onMessage(name);
    consumer.collect((event) => {
      const result = event.consumeMapNotNull<R>(mapNotNull);
      if (result !== undefined) {
        producer.send(result);
      }
    });
    producer.onClosed(() => {
      consumer.close();
    });
    return producer;
  }
  #requestProducer = new CacheGetter(() =>
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

  // onFetch(name: string, ...handlers: $OnFetch[]) {
  //   const onRequest = createFetchHandler(handlers);
  //   return onRequest.extendsTo(
  //     this.onRequest(name).collect((requestEvent) => {
  //       return onRequest(requestEvent.data);
  //     })
  //   );
  // }

  #responseProducer = new CacheGetter(() =>
    this.#messagePipeMap("response", (ipcMessage) => {
      if (ipcMessage instanceof IpcResponse) {
        return ipcMessage;
      }
    })
  );
  onResponse(name: string) {
    return this.#responseProducer.value.consumer(name);
  }
  #streamProducer = new CacheGetter(() =>
    this.#messagePipeMap("stream", (ipcMessage) => {
      if ("stream_id" in ipcMessage) {
        return ipcMessage;
      }
    })
  );
  onStream(name: string) {
    return this.#streamProducer.value.consumer(name);
  }
  #eventProducer = new CacheGetter(() =>
    this.#messagePipeMap("event", (ipcMessage) => {
      if (ipcMessage.type === IPC_MESSAGE_TYPE.EVENT) {
        return ipcMessage;
      }
    })
  );
  onEvent(name: string) {
    return this.#eventProducer.value.consumer(name);
  }
  #errorProducer = new CacheGetter(() =>
    this.#messagePipeMap("error", (ipcMessage) => {
      if (ipcMessage.type === IPC_MESSAGE_TYPE.ERROR) {
        return ipcMessage;
      }
    })
  );
  onError(name: string) {
    return this.#errorProducer.value.consumer(name);
  }

  #reqResMap = new CacheGetter(() => {
    const reqResMap = new Map<number, PromiseOut<IpcResponse>>();
    this.onResponse(`${this.debugId}-reqResMap-onResponse`).collect((event) => {
      const response = event.consume();
      const result = mapHelper.getAndRemove(reqResMap, response.reqId);
      if (result === undefined) {
        throw new Error(`no found response by reqId: ${event.data.reqId}`);
      }
      result.resolve(response);
    });
    return reqResMap;
  });

  /** 发起请求并等待响应 */
  request(url: IpcClientRequest): Promise<IpcResponse>;
  request(url: string, init?: $IpcRequestInit): Promise<IpcResponse>;
  async request(input: string | IpcClientRequest, init?: $IpcRequestInit) {
    const ipcRequest = input instanceof IpcClientRequest ? input : await this.#buildIpcRequest(input, init);
    const result = this.#registerReqId(ipcRequest.reqId);
    this.postMessage(ipcRequest);
    return result.promise;
  }
  /** 自定义注册 请求与响应 的id */
  #registerReqId(reqId = this.#allocReqId()) {
    return mapHelper.getOrPut(this.#reqResMap.value, reqId, () => new PromiseOut());
  }
  #buildIpcRequest(url: string, init?: $IpcRequestInit) {
    const reqId = this.#allocReqId();
    const ipcRequest = IpcClientRequest.fromRequest(reqId, this, url, init);
    return ipcRequest;
  }
  #allocReqId() {
    return this.#reqIdAcc++;
  }

  //#endregion

  async postMessage(message: $IpcMessage) {
    try {
      await this.awaitOpen("then-postMessage");
    } catch (e) {
      this.console.error(`ipc(${this}) fail to poseMessage: ${e}`);
      return;
    }
    this.endpoint.postIpcMessage(endpointIpcMessage(this.pid, message));
  }

  //#endregion

  //#region close

  @once()
  private get _closePo() {
    return new PromiseOut<string | undefined>();
  }
  awaitClosed() {
    return this._closePo.promise;
  }
  @once()
  get onClosed() {
    return promiseAsSignalListener(this._closePo.promise);
  }
  get isClosed() {
    return this.lifecycle.state.name == IPC_LIFECYCLE_STATE.CLOSED;
  }

  #closeOnce = $once(async (cause?: string) => {
    cause = cause ? cause.toString() : undefined;
    this.console.verbose("closing", cause);
    {
      const closing = IpcLifecycle(IpcLifecycleClosing(cause));
      this.#lifecycleLocaleFlow.emit(closing);
      this.#sendLifecycleToRemote(closing);
    }
    await this.#messageProducer.producer.close(cause);
    this._closePo.resolve(cause);
    {
      const closed = IpcLifecycle(IpcLifecycleClosed(cause));
      this.#lifecycleLocaleFlow.emitAndClear(closed);
      this.#sendLifecycleToRemote(closed);
    }
  });

  async close(cause?: string) {
    await this.#closeOnce(cause);
  }

  prepareChannel(headers: IpcHeaders, channelIpc?: Ipc | Promise<Ipc>) {
    return prepareIpcChannel(this, headers, channelIpc);
  }
  /**----- close end*/
}
export type $IpcRequestInit = {
  method?: string;
  body?: /* json+text */
  | null
    | string
    /* base64 */
    | ArrayBuffer
    | ArrayBufferView
    /* stream+base64 */
    | Blob
    | ReadableStream<Uint8Array>;
  headers?: IpcHeaders | HeadersInit;
};
