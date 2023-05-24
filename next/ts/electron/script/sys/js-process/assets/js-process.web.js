"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.APIS = void 0;
const dntShim = __importStar(require("../../../_dnt.shims.js"));
const createSignal_js_1 = require("../../../helper/createSignal.js");
const PromiseOut_js_1 = require("../../../helper/PromiseOut.js");
/// 这个文件是用在 js-process.html 的主线程中直接运行的，用来协调 js-worker 与 native 之间的通讯
// 也可以用在其他的 .html 文件中 但是内容需要部分的修改 
// 如果我们使用其他的 ***.html 文件作为渲染进程总的主线程，同样需要用这个来协调 js-worker 同 native 之间通信；
const ALL_PROCESS_MAP = new Map();
let acc_process_id = 0;
const allocProcessId = () => acc_process_id++;
/**
 * 创建 woker 线程 同样是当做一个程序角色
 * @param env_script_url 需要导入的环境目录 *** bootstramp.js 也就是 js-prcess.woker.cts 代码部分
 * @param metadata_json 对应的 JsMicroModurl 的 元数据 一般包括 mmid 和 worker.js 的位置
 * @param env_json
 * @param fetch_port messagePort 通过 js.sys.dweb 传递进来的 transfer
 * @param name woker 的名称
 * @returns
 */
const createProcess = async (env_script_url, metadata_json, env_json, fetch_port, name = new URL(env_script_url).hostname) => {
    const process_id = allocProcessId();
    const worker_url = URL.createObjectURL(new Blob([
        `import("${env_script_url}")
        .then(
          async({installEnv,Metadata})=>{
            void installEnv(new Metadata(${metadata_json},${env_json}));
            postMessage("ready")
          },
          (err)=>postMessage("ERROR:"+err)
        )`,
    ], {
        // esm 代码必须有正确的 mime
        type: "text/javascript",
    }));
    /// https://caniuse.com/mdn-api_worker_worker_ecmascript_modules 需要 2019 年之后的 WebView 支持： Safari 15+ || Chrome 80+
    const worker = new Worker(worker_url, {
        type: "module",
        name: name,
    });
    await new Promise((resolve, reject) => {
        worker.addEventListener("message", (event) => {
            if (event.data === "ready") {
                resolve();
            }
            else {
                reject(event.data);
            }
        }, { once: true });
    });
    worker.postMessage(["fetch-ipc-channel", fetch_port], [fetch_port]);
    /// 等待启动任务完成
    const env_ready_po = new PromiseOut_js_1.PromiseOut();
    const onEnvReady = (event) => {
        if (Array.isArray(event.data) && event.data[0] === "env-ready") {
            env_ready_po.resolve();
        }
    };
    worker.addEventListener("message", onEnvReady);
    await env_ready_po.promise;
    worker.removeEventListener("message", onEnvReady);
    /// 保存 js-process
    ALL_PROCESS_MAP.set(process_id, { worker, fetch_port });
    /// 触发事件
    // 这个触发可有可无，只有在调试阶段 才需要
    on_create_process_signal.emit({
        process_id,
        env_script_url,
    });
    /// TODO 使用 weaklock 来检测线程是否唤醒
    /// 这些是 js 的对象，返回是要返回到 原生环境里
    /// 在迁移到原生 Android 时：
    /// 可以通过 evalJs 获取 process_id、或者执行 runMain
    return {
        process_id,
    };
};
/**
 * 创建 ipc 通信 接受从 js.sys.dweb 传递寄哪里的 port
 * @param process_id
 * @param mmid
 * @param ipc_port
 * @param env_json
 * @returns
 */
const createIpc = async (process_id, mmid, ipc_port, env_json = '{}') => {
    const process = _forceGetProcess(process_id);
    process.worker.postMessage(["ipc-connect", mmid, env_json], [ipc_port]);
    /// 等待连接任务完成
    const connect_ready_po = new PromiseOut_js_1.PromiseOut();
    const onEnvReady = (event) => {
        if (Array.isArray(event.data) &&
            event.data[0] === "ipc-connect-ready" &&
            event.data[1] === mmid) {
            connect_ready_po.resolve();
        }
    };
    process.worker.addEventListener("message", onEnvReady);
    await connect_ready_po.promise;
    process.worker.removeEventListener("message", onEnvReady);
    return;
};
// 根据 process_id 获取进程
const _forceGetProcess = (process_id) => {
    const process = ALL_PROCESS_MAP.get(process_id);
    if (process === undefined) {
        throw new Error(`no found worker by id: ${process_id}`);
    }
    return process;
};
/**
 * 通过 process.worker 发送 run-main 的消息
 * @param process_id
 * @param config
 */
const runProcessMain = (process_id, config) => {
    const process = _forceGetProcess(process_id);
    process.worker.postMessage(["run-main", config]);
};
/**
 * 彻底退出后端，即删除APP的worker
 * @param process_id
 */
const destroyProcess = (process_id) => {
    const process = _forceGetProcess(process_id);
    process.worker.terminate();
};
const on_create_process_signal = (0, createSignal_js_1.createSignal)();
// 这里到处的 APIS 会通过 expose() 导入到给主进程调用
exports.APIS = {
    createProcess,
    runProcessMain,
    createIpc,
    destroyProcess
};
Object.assign(dntShim.dntGlobalThis, exports.APIS);
const html = String.raw;
on_create_process_signal.listen(({ process_id, env_script_url }) => {
    document.body.innerHTML += html `<div>
    <span>PID:${process_id}</span>
    <span>URL:${env_script_url}</span>
  </div>`;
});
