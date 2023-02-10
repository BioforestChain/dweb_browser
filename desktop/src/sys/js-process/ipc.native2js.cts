import { MessagePortIpc } from "../../core/ipc-web/MessagePortIpc.cjs";
import type { NativeIpc } from "../../core/ipc.native.cjs";
import { IPC_ROLE } from "../../core/ipc/const.cjs";

export const ALL_IPC_CACHE = new Map<number, MessagePort>();
export const saveNative2JsIpcPort = (port: MessagePort) => {
  const port_id = all_ipc_id_acc++;
  ALL_IPC_CACHE.set(port_id, port);
  return port_id;
};
let all_ipc_id_acc = 0;

/**
 * 在NW.js里，Native2JsIpc 几乎等价于 NativeIPC，都是使用原生的 MessagePort 即可
 * 差别只在于 Native2JsIpc 的远端是在 js-worker 中的
 *
 * ### 原理
 * 连接发起方执行 `fetch('file://js.sys.dweb/create-ipc')` 后，
 * 由 js-worker 创建了 channel-port1/2，然后 js-process(native) 负责中转这个信道（在nwjs中，我们直接使用内存引用，在mobile中，我们需要拦截webRequest），并为其存下一个 id(number)。
 * 最终将这个 id 通过 fetch 返回值返回。
 *
 * 那么连接发起方就可以通过这个 id(number) 和 Native2JsIpc 构造器来实现与 js-worker 的直连
 */
export class Native2JsIpc extends MessagePortIpc {
  constructor(
    port_id: number,
    remote: NativeIpc["remote"],
    role = IPC_ROLE.CLIENT,
    /** 这里是native直接通往js线程里的通讯，所以默认只支持字符串 */
    support_message_pack = false
  ) {
    const port = ALL_IPC_CACHE.get(port_id);
    if (port === undefined) {
      throw new Error(`no found port2(js-process) by id: ${port_id}`);
    }
    super(port, remote, role, support_message_pack);
    /// TODO 这里应该放在和 ALL_IPC_CACHE.set 同一个函数下，只是原生的 MessageChannel 没有 close 事件，这里没有给它模拟，所以有问题
    this.onClose(() => {
      ALL_IPC_CACHE.delete(port_id);
    });
  }
}
