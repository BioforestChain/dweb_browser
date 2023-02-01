"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JsIpc = exports.JsProcessNMM = void 0;
const helper_cjs_1 = require("../core/helper.cjs");
const ipc_native_cjs_1 = require("../core/ipc.native.cjs");
const micro_module_native_cjs_1 = require("../core/micro-module.native.cjs");
/**
 * 将指定的js运行在后台的一个管理器，
 * 注意它们共享一个域，所以要么就关闭
 */
class JsProcessNMM extends micro_module_native_cjs_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = `js.sys.dweb`;
        // static singleton = once(() => new JsProcessManager());
    }
    async _bootstrap() {
        const window = (this.window = await (0, helper_cjs_1.openNwWindow)("../../js-process.html", {
            show: true,
        }));
        if (window.window.APIS_READY !== true) {
            await new Promise((resolve) => {
                window.window.addEventListener("apis-ready", resolve);
            });
        }
        const apis = window.window;
        /// 创建 web worker
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/create-process",
            matchMode: "full",
            input: { main_code: "string" },
            output: "number",
            hanlder: (args) => {
                return apis.createWorker(args.main_code, (ipcMessage) => {
                    if (ipcMessage.type === 0 /* IPC_DATA_TYPE.REQUEST */) {
                        /// 收到 Worker 的数据请求，转发出去
                        this.fetch(ipcMessage.url, ipcMessage);
                    }
                });
            },
        });
        /// 创建 web 通讯管道
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/create-ipc",
            matchMode: "full",
            input: { process_id: "number" },
            output: "number",
            hanlder: (args) => {
                const port2 = apis.createIpc(args.process_id);
                const port_id = all_ipc_id_acc++;
                ALL_IPC_CACHE.set(port_id, port2);
                return port_id;
            },
        });
    }
    async _shutdown() { }
}
exports.JsProcessNMM = JsProcessNMM;
const ALL_IPC_CACHE = new Map();
let all_ipc_id_acc = 0;
const getIpcCache = (port_id) => {
    const port = ALL_IPC_CACHE.get(port_id);
    if (port === undefined) {
        throw new Error(`no found port2(js-process) by id: ${port_id}`);
    }
    return port;
};
/**
 * 在NW.js里，JsIpc几乎等价于 NativeIPC，都是使用原生的 MessagePort 即可
 * 差别只在于 JsIpc 的远端是在 js-worker 中的
 *
 * ### 原理
 * 连接发起方执行 `fetch('file://js.sys.dweb/create-ipc')` 后，
 * 由 js-worker 创建了 channel-port1/2，然后 js-process(native) 负责中转这个信道（在nwjs中，我们直接使用内存引用，在mobile中，我们需要拦截webRequest），并为其存下一个 id(number)。
 * 最终将这个 id 通过 fetch 返回值返回。
 *
 * 那么连接发起方就可以通过这个 id(number) 和 JsIpc 构造器来实现与 js-worker 的直连
 */
class JsIpc extends ipc_native_cjs_1.NativeIpc {
    constructor(port_id) {
        super(getIpcCache(port_id));
        /// TODO 这里应该放在和 ALL_IPC_CACHE.set 同一个函数下，只是原生的 MessageChannel 没有 close 事件，这里没有给它模拟，所以有问题
        this.onClose(() => {
            ALL_IPC_CACHE.delete(port_id);
        });
    }
}
exports.JsIpc = JsIpc;
