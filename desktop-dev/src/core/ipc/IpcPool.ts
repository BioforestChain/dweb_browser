import { createSignal } from "../../helper/createSignal.ts";
import { Ipc } from "../index.ts";

let ipc_pool_uid_acc = 0;

/**每一个worker 都会创建单独的IpcPool */
export class IpcPool {
  constructor(readonly poolName: string) {
    this.poolId = this.poolName + this.poolId;
  }

  readonly poolId = `-worker-${ipc_pool_uid_acc++}`;

  /**
   * 所有的ipc对象实例集合
   */
  #ipcSet = new Set<Ipc>();
  /**
   * 所有的委托进来的流的实例集合
   */
  #streamPool = new Map<String, ReadableStream>();

  /**安全的创建ipc */
  safeCreatedIpc(ipc: Ipc, autoStart: boolean, startReason?: string) {
    /// 保存ipc，并且根据它的生命周期做自动删除
    this.#ipcSet.add(ipc);
    // 自动启动
    if (autoStart) {
      ipc.start(startReason ?? "autoStart");
    }
    ipc.onClosed(() => {
      this.#ipcSet.delete(ipc);
      console.log("ipcpool-remote-ipc", ipc);
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

// 这是一个简单的hashCode实现，用于计算字符串的hashCode
export function hashString(s: string): number {
  let hash = 0;
  for (let i = 0; i < s.length; i++) {
    // 使用charCodeAt获取字符的Unicode值，这个值在0-65535之间
    const charCode = s.charCodeAt(i);
    // 使用了一种称为“旋转哈希”的技术，通过将上一个哈希值左旋然后加上新字符的哈希值来生成新的哈希值
    hash = (hash << 5) - hash + charCode;
    // 使用按位异或运算符将hash值限制在一个32位的整数范围内
    hash = hash & hash;
  }
  return hash;
}

export const workerIpcPool = new IpcPool("desktop");
