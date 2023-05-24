"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Native2JsIpc = exports.saveNative2JsIpcPort = exports.ALL_IPC_CACHE = void 0;
const MessagePortIpc_js_1 = require("../../core/ipc-web/MessagePortIpc.js");
const const_js_1 = require("../../core/ipc/const.js");
/**
 * 单例模式用来保存全部的, port 发送给 woker.js 的对应 port
 */
exports.ALL_IPC_CACHE = new Map();
const saveNative2JsIpcPort = (port) => {
    const port_id = all_ipc_id_acc++;
    exports.ALL_IPC_CACHE.set(port_id, port);
    return port_id;
};
exports.saveNative2JsIpcPort = saveNative2JsIpcPort;
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
class Native2JsIpc extends MessagePortIpc_js_1.MessagePortIpc {
    constructor(port_id, remote, role = const_js_1.IPC_ROLE.CLIENT) {
        const port = exports.ALL_IPC_CACHE.get(port_id);
        if (port === undefined) {
            throw new Error(`no found port2(js-process) by id: ${port_id}`);
        }
        super(port, remote, role);
        /// TODO 这里应该放在和 ALL_IPC_CACHE.set 同一个函数下，只是原生的 MessageChannel 没有 close 事件，这里没有给它模拟，所以有问题
        this.onClose(() => {
            exports.ALL_IPC_CACHE.delete(port_id);
        });
    }
}
exports.Native2JsIpc = Native2JsIpc;
