import { createSignal } from "@dweb-browser/helper/createSignal.ts";
import { CUSTOM_INSPECT, logger } from "@dweb-browser/helper/logger.ts";
import type { $MicroModuleManifest } from "../types.ts";
import { IpcEndpoint } from "./endpoint/IpcEndpoint.ts";
import { Ipc } from "./ipc.ts";

/**每一个worker 都会创建单独的IpcPool */
export class IpcPool {
  constructor(readonly poolId = `js-${crypto.randomUUID()}`) {}
  toString() {
    return `IpcPool#${this.poolId}`;
  }
  [CUSTOM_INSPECT]() {
    return this.toString();
  }
  readonly console = logger(this);
  /**
   * 所有的ipc对象实例集合
   */
  #ipcSet = new Set<Ipc>();
  /**
   * 所有的委托进来的流的实例集合
   */
  #streamPool = new Map<string, ReadableStream>();

  /**安全的创建ipc */
  createIpc(
    endpoint: IpcEndpoint,
    pid: number,
    locale: $MicroModuleManifest,
    remote: $MicroModuleManifest,
    autoStart = false,
    startReason?: string
  ) {
    const ipc = new Ipc(pid, endpoint, locale, remote, this);
    this.safeCreatedIpc(ipc, autoStart, startReason);
    return ipc;
  }

  safeCreatedIpc(ipc: Ipc, autoStart: boolean, startReason?: string) {
    /// 保存ipc，并且根据它的生命周期做自动删除
    this.#ipcSet.add(ipc);
    // 自动启动
    if (autoStart) {
      void ipc.start(true, startReason ?? "autoStart");
    }
    ipc.onClosed(() => {
      this.#ipcSet.delete(ipc);
      this.console.debug("ipcpool-remote-ipc", ipc);
    });
  }

  //#region  close
  #destroySignal = createSignal();
  onDestory = this.#destroySignal.listen;
  async destroy() {
    this.#destroySignal.emit();
    for (const _ipc of this.#ipcSet) {
      await _ipc.close();
    }
    this.#ipcSet.clear();
  }
  // close end
}

/**
 * 通常来说，一个隔离环境中，有且仅有一个池子
 */
export const jsIpcPool = new IpcPool();
