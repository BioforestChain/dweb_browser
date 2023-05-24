import { MessagePortIpc } from "../../core/ipc-web/MessagePortIpc.js";
import type { NativeIpc } from "../../core/ipc.native.js";
import { IPC_ROLE } from "../../core/ipc/const.js";
/**
 * 单例模式用来保存全部的, port 发送给 woker.js 的对应 port
 */
export declare const ALL_IPC_CACHE: Map<number, MessagePort>;
export declare const saveNative2JsIpcPort: (port: MessagePort) => number;
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
export declare class Native2JsIpc extends MessagePortIpc {
    constructor(port_id: number, remote: NativeIpc["remote"], role?: IPC_ROLE);
}
