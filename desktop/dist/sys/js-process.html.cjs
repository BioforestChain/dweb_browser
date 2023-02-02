"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.APIS = void 0;
const helper_cjs_1 = require("../core/helper.cjs");
const ipc_native_cjs_1 = require("../core/ipc.native.cjs");
const JS_PROCESS_WORKER_CODE = fetch(new URL("dist/sys/js-process.worker.cjs", location.href)).then((res) => res.text());
/// 这个文件是用在 js-process.html 的主线程中直接运行的，用来协调 js-worker 与 native 之间的通讯
const ALL_PROCESS_MAP = new Map();
let acc_process_id = 0;
const createProcess = async (module, main_code, onMessage) => {
    const process_id = acc_process_id++;
    const worker = new Worker(`data:utf-8,
   ((module,exports=module.exports)=>{${await JS_PROCESS_WORKER_CODE};return module.exports})({exports:{}}).installEnv();
   ((module,exports=module.exports)=>{${main_code}})({exports:{}});`.replaceAll(`"use strict";`, ""));
    /// 一些启动任务
    const ipc_port_po = new helper_cjs_1.PromiseOut();
    const onIpcChannelConnected = (event) => {
        if (Array.isArray(event.data) && event.data[0] === "fetch-ipc-channel") {
            ipc_port_po.resolve(event.data[1]);
        }
    };
    /// 等待启动任务完成
    worker.addEventListener("message", onIpcChannelConnected);
    const ipc = new ipc_native_cjs_1.NativeIpc(await ipc_port_po.promise, module);
    worker.removeEventListener("message", onIpcChannelConnected);
    /// 保存 js-process
    ALL_PROCESS_MAP.set(process_id, { worker, ipc });
    /// 绑定监听
    ipc.onMessage(onMessage);
    /// TODO 使用 weaklock 来检测线程是否唤醒
    return process_id;
};
const createIpc = (worker_id) => {
    const process = ALL_PROCESS_MAP.get(worker_id);
    if (process === undefined) {
        throw new Error(`no found worker by id: ${worker_id}`);
    }
    const channel = new MessageChannel();
    process.worker.postMessage(["ipc-channel", channel.port2], [channel.port2]);
    return channel.port1;
};
exports.APIS = {
    createProcess: createProcess,
    createIpc,
};
