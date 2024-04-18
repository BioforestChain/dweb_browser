/**
 * 通常我们将先进入 opened 状态的称为 endpoint-0，其次是 endpoint-1
 */

import { once } from "../../../helper/$once.ts";
import { PromiseOut } from "../../../helper/PromiseOut.ts";
import { StateSignal, type $ReadyonlyStateSignal } from "../../../helper/StateSignal.ts";
import { mapHelper } from "../../../helper/mapHelper.ts";
import { setHelper } from "../../../helper/setHelper.ts";
import { Producer } from "../../helper/Producer.ts";
import type { $IpcMessage } from "../ipc-message/IpcMessage.ts";
import { IPC_MESSAGE_TYPE } from "../ipc-message/internal/IpcMessage.ts";
import { Ipc } from "../ipc.ts";
import type { $EndpointIpcMessage } from "./EndpointIpcMessage.ts";
import {
  ENDPOINT_LIFECYCLE_STATE,
  ENDPOINT_PROTOCOL,
  endpointLifecycle,
  endpointLifecycleClosed,
  endpointLifecycleClosing,
  endpointLifecycleInit,
  endpointLifecycleOpend,
  endpointLifecycleOpening,
  type $EndpointLifecycle,
} from "./EndpointLifecycle.ts";

export abstract class IpcEndpoint {
  private accPid = 0;

  /**
   * 注册一个Pid
   * endpoint-0 的 ipc-fork 出来的 pid 是偶数
   * endpoint-1 的 ipc-fork 出来的 pid 是奇数
   */
  generatePid = () => (this.accPid += 2);
  // 调试标记🏷️
  abstract debugId: string;

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

  getIpcMessageProducer(pid: number) {
    return mapHelper.getOrPut(this.ipcMessageProducers, pid, () => this.ipcMessageProducer(pid));
  }

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
  protected lifecycleLocaleFlow = new StateSignal<$EndpointLifecycle>(endpointLifecycle(endpointLifecycleInit()));
  /**
   * 生命周期 监听器
   */
  onLifeCycle = this.lifecycleLocaleFlow.listen;
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

  /**启动 */
  startOnce = once(async () => {
    console.log("startOnce", this.lifecycle);
    await this.doStart();
    let localeSubProtocols = this.getLocaleSubProtocols();
    // 当前状态必须是从init开始
    if (this.lifecycle.state.name === ENDPOINT_LIFECYCLE_STATE.INIT) {
      const opening = endpointLifecycle(endpointLifecycleOpening(localeSubProtocols));
      this.sendLifecycleToRemote(opening);
      console.log("emit-locale-lifecycle", opening);
      this.lifecycleLocaleFlow.emit(opening);
    } else {
      throw new Error(`endpoint state=${this.lifecycle}`);
    }
    // 监听远端生命周期指令，进行协议协商
    this.lifecycleRemoteFlow.listen((lifecycle) => {
      console.log("remote_lifecycle-in", lifecycle);
      switch (lifecycle.state.name) {
        case ENDPOINT_LIFECYCLE_STATE.CLOSING:
        case ENDPOINT_LIFECYCLE_STATE.CLOSED: {
          this.close();
          break;
        }
        // 收到 opened 了，自己也设置成 opened，代表正式握手成功
        case ENDPOINT_LIFECYCLE_STATE.OPENED: {
          const lifecycleLocale = this.lifecycle;
          if (lifecycleLocale.state.name === ENDPOINT_LIFECYCLE_STATE.OPENED) {
            const state = endpointLifecycle(endpointLifecycleOpend(lifecycleLocale.state.subProtocols));
            this.sendLifecycleToRemote(state);
            console.log("emit-locale-lifecycle", state);
            this.lifecycleLocaleFlow.emit(state);
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
          let nextState: $EndpointLifecycle;
          if ([...localeSubProtocols].sort().join() !== lifecycle.state.subProtocols.slice().sort().join()) {
            localeSubProtocols = setHelper.intersect(localeSubProtocols, lifecycle.state.subProtocols);
            const opening = endpointLifecycle(endpointLifecycleOpening(localeSubProtocols));
            this.lifecycleLocaleFlow.emit(opening);
            nextState = opening;
          } else {
            nextState = endpointLifecycle(endpointLifecycleOpend(localeSubProtocols));
          }
          this.sendLifecycleToRemote(nextState);
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
    const off = this.onLifeCycle((lifecycle) => {
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
    console.log("awaitOpen", lifecycle, reason);
    off();
    return lifecycle;
  }

  //#region Close

  private _isClose = false;
  get isClose() {
    return this._isClose;
  }

  async close() {
    this._isClose = true;
    await this.doClose();
  }

  async doClose(cause?: string) {
    switch (this.lifecycle.state.name) {
      case ENDPOINT_LIFECYCLE_STATE.OPENED:
      case ENDPOINT_LIFECYCLE_STATE.OPENING: {
        this.sendLifecycleToRemote(endpointLifecycle(endpointLifecycleClosing()));
        break;
      }
      case ENDPOINT_LIFECYCLE_STATE.CLOSED: {
        return;
      }
    }
    this.beforeClose?.();
    /// 关闭所有的子通道
    for (const channel of this.ipcMessageProducers.values()) {
      await channel.producer.close(cause);
    }
    this.ipcMessageProducers.clear();
    this.sendLifecycleToRemote(endpointLifecycle(endpointLifecycleClosed()));
    this.afterClosed?.();
  }

  protected beforeClose?: (cause?: string) => void;
  protected afterClosed?: (cause?: string) => void;

  //#endregion
}
