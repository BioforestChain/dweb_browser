/**
 * 通常我们将先进入 opened 状态的称为 endpoint-0，其次是 endpoint-1
 */

import { Producer } from "@dweb-browser/helper/Producer.ts";
import { PromiseOut } from "@dweb-browser/helper/PromiseOut.ts";
import { StateSignal, type $ReadyonlyStateSignal } from "@dweb-browser/helper/StateSignal.ts";
import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { mapHelper } from "@dweb-browser/helper/fun/mapHelper.ts";
import { setHelper } from "@dweb-browser/helper/fun/setHelper.ts";
import { CUSTOM_INSPECT, logger } from "@dweb-browser/helper/logger.ts";
import type { $IpcMessage } from "../ipc-message/IpcMessage.ts";
import { IPC_MESSAGE_TYPE } from "../ipc-message/internal/IpcMessage.ts";
import type { Ipc } from "../ipc.ts";
import type { $EndpointIpcMessage } from "./EndpointIpcMessage.ts";
import {
  ENDPOINT_LIFECYCLE_STATE,
  ENDPOINT_PROTOCOL,
  EndpointLifecycle,
  endpointLifecycleClosed,
  endpointLifecycleClosing,
  endpointLifecycleInit,
  endpointLifecycleOpend,
  endpointLifecycleOpening,
  type $EndpointLifecycle,
} from "./EndpointLifecycle.ts";

export abstract class IpcEndpoint {
  readonly console = logger(this);
  constructor(readonly debugId: string) {}
  abstract toString(): string;
  [CUSTOM_INSPECT]() {
    return this.toString();
  }

  private accPid = 0;

  /**
   * 注册一个Pid
   * endpoint-0 的 ipc-fork 出来的 pid 是偶数
   * endpoint-1 的 ipc-fork 出来的 pid 是奇数
   */
  generatePid = () => (this.accPid += 2);

  /**
   * 发送消息
   */
  abstract postIpcMessage(msg: $EndpointIpcMessage): Promise<void>;

  /**
   * @internal
   * 获取消息管道
   */
  private ipcMessageProducers = new Map<number, ReturnType<IpcEndpoint["ipcMessageProducer"]>>();
  private ipcMessageProducer(pid: number) {
    const ipcPo = new PromiseOut<Ipc>();
    const producer = new Producer<$IpcMessage>(`ipc-msg/${this.debugId}/${pid}`);
    const consumer = producer.consumer("watch-fork");
    consumer.collect((event) => {
      if (event.data.type === IPC_MESSAGE_TYPE.FORK) {
        this.accPid = Math.max(event.data.pid - 1, this.accPid);
      }
    });
    producer.onClosed(() => {
      this.ipcMessageProducers.delete(pid);
    });
    return { pid, ipcPo, producer };
  }

  /**获取ipc消息的生产者 */
  getIpcMessageProducer(pid: number) {
    return mapHelper.getOrPut(this.ipcMessageProducers, pid, () => this.ipcMessageProducer(pid));
  }
  /**通过ipc拿到ipc消息生产者对象 */
  getIpcMessageProducerByIpc(ipc: Ipc) {
    const result = this.getIpcMessageProducer(ipc.pid);
    ipc.onClosed(() => {
      this.ipcMessageProducers.delete(ipc.pid);
    });
    result.ipcPo.resolve(ipc);
    return result;
  }

  //#endregion

  //#region EndpointLifecycle
  // 这里的设计相对复杂，因为提供了内置的生命周期相关的实现，包括 握手、关闭
  // 所以这里的 IO 需要通过子类提供的两个 StateFlow 对象来代表

  /**
   * 本地的生命周期状态流
   */
  protected lifecycleLocaleFlow = new StateSignal<$EndpointLifecycle>(
    EndpointLifecycle(endpointLifecycleInit()),
    EndpointLifecycle.equals
  );
  /**
   * 生命周期 监听器
   */
  onLifecycle = this.lifecycleLocaleFlow.listen;
  /**
   * 当前生命周期
   */
  get lifecycle() {
    return this.lifecycleLocaleFlow.state;
  }

  /**
   * 远端的生命周期状态流
   */
  protected abstract lifecycleRemoteFlow: $ReadyonlyStateSignal<$EndpointLifecycle>;

  /**
   * 向远端发送 生命周期 信号
   */
  protected abstract sendLifecycleToRemote(state: $EndpointLifecycle): void;

  /**
   * 是否处于可以发送消息的状态
   */
  get isActivity() {
    return ENDPOINT_LIFECYCLE_STATE.OPENED == this.lifecycle.state.name;
  }

  /**
   * 获取支持的协议，在协商的时候会用到
   */
  protected abstract getLocaleSubProtocols(): Set<ENDPOINT_PROTOCOL>;

  /**
   * 启动生命周期的相关的工作
   */
  abstract doStart(): void;

  async start(isAwait: boolean = true) {
    this.startOnce();
    if (isAwait) {
      await this.awaitOpen("from-start");
    }
  }
  readonly localSessionId = crypto.randomUUID();

  /**启动 */
  startOnce = $once(async () => {
    this.console.log("startOnce", this.lifecycle);
    await this.doStart();
    const localeSubProtocols = this.getLocaleSubProtocols();
    const localSessionId = this.localSessionId;
    // 当前状态必须是从init开始
    if (this.lifecycle.state.name === ENDPOINT_LIFECYCLE_STATE.INIT) {
      const opening = EndpointLifecycle(endpointLifecycleOpening(localeSubProtocols, [localSessionId]));
      this.sendLifecycleToRemote(opening);
      // this.console.log("emit-locale-lifecycle", opening);
      this.lifecycleLocaleFlow.emit(opening);
    } else {
      throw new Error(`endpoint state=${this.lifecycle.state.name}`);
    }
    // 监听远端生命周期指令，进行协议协商
    this.lifecycleRemoteFlow.listen((remoteLifecycle) => {
      const remoteState = remoteLifecycle.state;
      this.console.verbose("remote-lifecycle-in", remoteState);
      switch (remoteState.name) {
        case ENDPOINT_LIFECYCLE_STATE.CLOSING:
        case ENDPOINT_LIFECYCLE_STATE.CLOSED: {
          this.close();
          break;
        }
        // 收到 opened 了，自己也设置成 opened，代表正式握手成功
        case ENDPOINT_LIFECYCLE_STATE.OPENED: {
          const localState = this.lifecycle.state;
          // this.console.log("remote-opend-&-locale-lifecycle", lifecycleLocale);
          if (localState.name === ENDPOINT_LIFECYCLE_STATE.OPENING) {
            const opend = EndpointLifecycle(
              endpointLifecycleOpend(localState.subProtocols, localState.sessionIds.join("~"))
            );
            this.sendLifecycleToRemote(opend);
            // this.console.log("emit-locale-lifecycle", opend);
            this.lifecycleLocaleFlow.emit(opend);
            /// 后面被链接的ipc，pid从奇数开始
            this.accPid++;
          }
          break;
        }
        // 如果对方是 init，代表刚刚初始化，那么发送目前自己的状态
        case ENDPOINT_LIFECYCLE_STATE.INIT: {
          this.sendLifecycleToRemote(this.lifecycle);
          break;
        }
        // 等收到对方 Opening ，说明对方也开启了，那么开始协商协议，直到一致后才进入 Opened
        case ENDPOINT_LIFECYCLE_STATE.OPENING: {
          const localState = this.lifecycle.state;
          if (localState.name === ENDPOINT_LIFECYCLE_STATE.OPENING) {
            let nextState: $EndpointLifecycle;
            if (EndpointLifecycle.stateEquals(localState, remoteState) === false) {
              const subProtocols = setHelper.intersect(localeSubProtocols, remoteState.subProtocols);
              const sessionIds = setHelper.sort(
                setHelper.union(localState.sessionIds, remoteState.sessionIds),
                (a, b) => a.localeCompare(b)
              );
              const opening = EndpointLifecycle(endpointLifecycleOpening(subProtocols, sessionIds));
              this.lifecycleLocaleFlow.emit(opening);
              nextState = opening;
            } else {
              nextState = EndpointLifecycle(
                endpointLifecycleOpend(localState.subProtocols, localState.sessionIds.join("~"))
              );
            }
            this.sendLifecycleToRemote(nextState);
          }
          break;
        }
      }
    });
  });

  /**等待启动 */
  async awaitOpen(reason?: string) {
    if (this.lifecycle.state.name == ENDPOINT_LIFECYCLE_STATE.OPENED) {
      return this.lifecycle;
    }
    const op = new PromiseOut<$EndpointLifecycle>();
    const off = this.onLifecycle((lifecycle) => {
      switch (lifecycle.state.name) {
        case ENDPOINT_LIFECYCLE_STATE.OPENED: {
          op.resolve(lifecycle);
          break;
        }
        case (ENDPOINT_LIFECYCLE_STATE.CLOSED, ENDPOINT_LIFECYCLE_STATE.CLOSING): {
          op.reject("endpoint already closed");
          break;
        }
      }
    });
    const lifecycle = await op.promise;
    this.console.log("awaitOpen", lifecycle, reason);
    off();
    return lifecycle;
  }

  //#region Close

  protected closePo = new PromiseOut<string | undefined>();
  private _isClose = false;
  get isClose() {
    return this._isClose;
  }

  async close(cause?: string) {
    this._isClose = true;
    await this.doClose(cause);
  }

  async doClose(cause?: string) {
    switch (this.lifecycle.state.name) {
      case ENDPOINT_LIFECYCLE_STATE.OPENED:
      case ENDPOINT_LIFECYCLE_STATE.OPENING: {
        this.sendLifecycleToRemote(EndpointLifecycle(endpointLifecycleClosing()));
        break;
      }
      case ENDPOINT_LIFECYCLE_STATE.CLOSED: {
        return;
      }
    }
    this.closePo.resolve(cause);
    this.beforeClose?.();
    /// 关闭所有的子通道
    for (const channel of this.ipcMessageProducers.values()) {
      await channel.producer.close(cause);
    }
    this.ipcMessageProducers.clear();
    this.sendLifecycleToRemote(EndpointLifecycle(endpointLifecycleClosed()));
    this.afterClosed?.();
  }

  protected beforeClose?: (cause?: string) => void;
  protected afterClosed?: (cause?: string) => void;

  //#endregion
}
