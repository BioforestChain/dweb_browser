/**
 * 通常我们将先进入 opened 状态的称为 endpoint-0，其次是 endpoint-1
 */

import { once } from "../../../helper/$once.ts";
import { PromiseOut } from "../../../helper/PromiseOut.ts";
import { $ReadyonlyStateSignal, StateSignal } from "../../../helper/StateSignal.ts";
import { mapHelper } from "../../../helper/mapHelper.ts";
import { Producer } from "../../helper/Producer.ts";
import { ENDPOINT_LIFECYCLE_STATE } from "../helper/const.ts";
import { $IpcMessage, IPC_MESSAGE_TYPE } from "../index.ts";
import { EndpointLifecycle, EndpointProtocol } from "./EndpointLifecycle.ts";
import { EndpointIpcMessage } from "./EndpointMessage.ts";

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
  abstract postIpcMessage(msg: EndpointIpcMessage): Promise<void>;

  /**
   * 获取消息管道
   */

  private ipcMessageProducers = new Map<number, Producer<$IpcMessage>>();

  getIpcMessageProducer = (pid: number) =>
    mapHelper.getOrPut(this.ipcMessageProducers, pid, () => {
      const producer = new Producer<$IpcMessage>(`ipc-msg/${this.debugId}/${pid}`);
      producer.consumer("watch-fork").collect((event) => {
        if (event.data.type == IPC_MESSAGE_TYPE.FORK) {
          const accPid = this.accPid;
          Math.max(accPid, event.data.pid - 1);
        }
      });
      producer.onClose(() => {
        this.ipcMessageProducers.delete(pid);
      });
      return producer;
    });

  //#endregion

  //#region EndpointLifecycle
  // 这里的设计相对复杂，因为提供了内置的生命周期相关的实现，包括 握手、关闭
  // 所以这里的 IO 需要通过子类提供的两个 StateFlow 对象来代表

  /**
   * 本地的生命周期状态流
   */
  protected lifecycleLocaleFlow = new StateSignal(EndpointLifecycle.init());
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
  protected abstract lifecycleRemoteFlow: $ReadyonlyStateSignal<EndpointLifecycle>;

  /**
   * 向远端发送 生命周期 信号
   */
  protected abstract sendLifecycleToRemote(state: EndpointLifecycle): void;

  /**
   * 是否处于可以发送消息的状态
   */
  get isActivity() {
    return ENDPOINT_LIFECYCLE_STATE.OPENED == this.lifecycle.state;
  }

  /**
   * 获取支持的协议，在协商的时候会用到
   */
  protected abstract getLocaleSubProtocols(): Set<EndpointProtocol>;

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
    console.log("js_startOnce", this.lifecycle);
    await this.doStart();
    let localeSubProtocols = this.getLocaleSubProtocols();
    // 当前状态必须是从init开始
    if (this.lifecycle.state === ENDPOINT_LIFECYCLE_STATE.INIT) {
      const opening = EndpointLifecycle.opening(localeSubProtocols);
      this.sendLifecycleToRemote(opening);
      console.log("js_emit-locale-lifecycle", opening);
      this.lifecycleLocaleFlow.emit(opening);
    } else {
      throw new Error(`endpoint state=${this.lifecycle}`);
    }
    // 监听远端生命周期指令，进行协议协商
    this.lifecycleRemoteFlow.listen((state) => {
      console.log("js_remote_lifecycle-in", state);
      switch (state.state) {
        case (ENDPOINT_LIFECYCLE_STATE.CLOSED, ENDPOINT_LIFECYCLE_STATE.CLOSING): {
          this.close();
          break;
        }
        // 收到 opened 了，自己也设置成 opened，代表正式握手成功
        case ENDPOINT_LIFECYCLE_STATE.OPENED: {
          if (this.lifecycle.state === ENDPOINT_LIFECYCLE_STATE.OPENED) {
            const state = EndpointLifecycle.opend(this.lifecycle.subProtocols);
            this.sendLifecycleToRemote(state);
            console.log("js_emit-locale-lifecycle", state);
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
          let nextState: EndpointLifecycle;
          if (localeSubProtocols != state.subProtocols) {
            localeSubProtocols = state.subProtocols;
            const opening = EndpointLifecycle.opening(localeSubProtocols);
            this.lifecycleLocaleFlow.emit(opening);
            nextState = opening;
          } else {
            nextState = EndpointLifecycle.opend(localeSubProtocols);
          }
          this.sendLifecycleToRemote(nextState);
          break;
        }
      }
    });
  });

  /**等待启动 */
  async awaitOpen(reason?: string) {
    if (this.lifecycle.state == ENDPOINT_LIFECYCLE_STATE.OPENED) {
      return this.lifecycle.state;
    }
    const op = new PromiseOut<ENDPOINT_LIFECYCLE_STATE>();
    const off = this.onLifeCycle((lifecycle) => {
      switch (lifecycle.state) {
        case ENDPOINT_LIFECYCLE_STATE.OPENED: {
          op.resolve(lifecycle.state);
          break;
        }
        case (ENDPOINT_LIFECYCLE_STATE.CLOSED, ENDPOINT_LIFECYCLE_STATE.CLOSING): {
          op.reject("endpoint already closed");
          break;
        }
      }
    });
    const lifecycle = await op.promise;
    console.log("js_awaitOpen", lifecycle, reason);
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
    switch (this.lifecycle.state) {
      case (ENDPOINT_LIFECYCLE_STATE.OPENED, ENDPOINT_LIFECYCLE_STATE.OPENING): {
        this.sendLifecycleToRemote(EndpointLifecycle.closing());
        break;
      }
      case ENDPOINT_LIFECYCLE_STATE.CLOSED: {
        return;
      }
    }
    this.beforeClose?.();
    /// 关闭所有的子通道
    for (const channel of this.ipcMessageProducers.values()) {
      await channel.close(cause);
    }
    this.ipcMessageProducers.clear();
    this.sendLifecycleToRemote(EndpointLifecycle.closed());
    this.afterClosed?.();
  }

  protected beforeClose?: (cause?: string) => void;
  protected afterClosed?: (cause?: string) => void;

  //#endregion
}
