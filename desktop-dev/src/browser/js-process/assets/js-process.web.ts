import { $Callback, createSignal } from "../../../helper/createSignal.ts";
import { PromiseOut } from "../../../helper/PromiseOut.ts";

class ChangeableMap<K, V> extends Map<K, V> {
  private _changeSignal = createSignal<$Callback<[this]>>();
  onChange = this._changeSignal.listen;
  override set(key: K, value: V) {
    if ((this.has(key) && this.get(key) === value) === false) {
      super.set(key, value);
      this._changeSignal.emit(this);
    }
    return this;
  }
  override delete(key: K): boolean {
    if (super.delete(key)) {
      this._changeSignal.emit(this);
      return true;
    }
    return false;
  }
  override clear(): void {
    if (this.size === 0) {
      return;
    }
    super.clear();
    this._changeSignal.emit(this);
  }
}

/// 这个文件是用在 js-process.html 的主线程中直接运行的，用来协调 js-worker 与 native 之间的通讯
// 也可以用在其他的 .html 文件中 但是内容需要部分的修改
// 如果我们使用其他的 ***.html 文件作为渲染进程总的主线程，同样需要用这个来协调 js-worker 同 native 之间通信；
const ALL_PROCESS_MAP = new ChangeableMap<
  number,
  {
    env_script_url: string;
    worker: Worker;
    fetch_port: MessagePort;
  }
>();
let acc_process_id = 0;
const allocProcessId = () => acc_process_id++;

/**
 * 创建 woker 线程 同样是当做一个程序角色
 * @param env_script_url 需要导入的环境目录 *** bootstramp.js 也就是 js-prcess.woker.cts 代码部分
 * @param metadata_json 对应的 JsMicroModurl 的 元数据 一般包括 mmid 和 worker.js 的位置
 * @param env_json
 * @param fetch_port messagePort 通过 js.browser.dweb 传递进来的 transfer
 * @param name woker 的名称
 * @returns
 */
const createProcess = async (
  env_script_url: string,
  metadata_json: string,
  env_json: string,
  fetch_port: MessagePort,
  name: string = new URL(env_script_url).hostname
) => {
  const process_id = allocProcessId();
  const worker_url = URL.createObjectURL(
    new Blob(
      [
        `import("${env_script_url}")
        .then(
          async({installEnv,Metadata})=>{
            void installEnv(new Metadata(${metadata_json},${env_json}));
            postMessage("ready")
          },
          (err)=>postMessage("ERROR:"+err)
        )`,
      ],
      {
        // esm 代码必须有正确的 mime
        type: "text/javascript",
      }
    )
  );
  /// https://caniuse.com/mdn-api_worker_worker_ecmascript_modules 需要 2019 年之后的 WebView 支持： Safari 15+ || Chrome 80+
  const worker = new Worker(worker_url, {
    type: "module",
    name: name,
  });

  await new Promise<void>((resolve, reject) => {
    worker.addEventListener(
      "message",
      (event) => {
        if (event.data === "ready") {
          resolve();
        } else {
          reject(event.data);
        }
      },
      { once: true }
    );
  });

  worker.postMessage(["fetch-ipc-channel", fetch_port], [fetch_port]);
  /// 等待启动任务完成
  const env_ready_po = new PromiseOut<void>();
  const onEnvReady = (event: MessageEvent) => {
    if (Array.isArray(event.data) && event.data[0] === "env-ready") {
      env_ready_po.resolve();
    }
  };
  worker.addEventListener("message", onEnvReady);
  await env_ready_po.promise;
  worker.removeEventListener("message", onEnvReady);

  /// 保存 js-process
  ALL_PROCESS_MAP.set(process_id, { worker, fetch_port, env_script_url });

  /// TODO 使用 weaklock 来检测线程是否唤醒

  /// 这些是 js 的对象，返回是要返回到 原生环境里
  /// 在迁移到原生 Android 时：
  /// 可以通过 evalJs 获取 process_id、或者执行 runMain

  return {
    process_id,
  };
};

/**
 * 创建 ipc 通信 接受从 js.browser.dweb 传递寄哪里的 port
 * @param process_id
 * @param mmid
 * @param ipc_port
 * @param env_json
 * @returns
 */
const createIpc = async (process_id: number, mmid: string, ipc_port: MessagePort, env_json = "{}") => {
  const process = _forceGetProcess(process_id);

  process.worker.postMessage(["ipc-connect", mmid, env_json], [ipc_port]);
  /// 等待连接任务完成
  const connect_ready_po = new PromiseOut<void>();
  const onEnvReady = (event: MessageEvent) => {
    if (Array.isArray(event.data) && event.data[0] === "ipc-connect-ready" && event.data[1] === mmid) {
      connect_ready_po.resolve();
    }
  };
  process.worker.addEventListener("message", onEnvReady);
  await connect_ready_po.promise;
  process.worker.removeEventListener("message", onEnvReady);
  return;
};

const createIpcFail = async (process_id: number, mmid: string, reason: string) => {
  const process = _forceGetProcess(process_id);
  process.worker.postMessage(["ipc-connect-fail", mmid, reason]);
};

// 根据 process_id 获取进程
const _forceGetProcess = (process_id: number) => {
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
const runProcessMain = (process_id: number, config: $RunMainConfig) => {
  const process = _forceGetProcess(process_id);
  process.worker.postMessage(["run-main", config]);
};

/**
 * 彻底退出后端，即删除APP的worker
 * @param process_id
 */
const destroyProcess = (process_id: number) => {
  const process = ALL_PROCESS_MAP.get(process_id);
  if(process === undefined){
    return false
  }
  /**
   * @TODO worker 可以主动推送一些信息过来，告知现在正在进行的一些事务的原因
   * 那么应该允许它们与用户进行一定的交互来提示用户正在中断一些用户预期之外的任务，如果用户执意中断，那么就正式中断，如：
   * ```ts
   * if(process.abortTerminateReason !== null){
   *   showWindow();
   *   /// 用户对这些理由进行了一些决策，askReason多次调用，会汇集到同一个视图中，不会过渡干扰
   *
   *   if(await askReason(process.abortTerminateReason) === FORCE_TERMINATE) {
   *      process.worker.terminate();
   *   }
   * }
   *
   * ```
   * 在执行该询问的时候，不会通知子进程，否则子进程可能会因此进行一些恶意的操作（或者是错误的操作）去使得自己再次被激活
   */
  process.worker.terminate();
  ALL_PROCESS_MAP.delete(process_id);
  return true;
};

type $OnCreateProcessMessage = (msg: { process_id: number; env_script_url: string }) => unknown;

// 这里到处的 APIS 会通过 expose() 导入到给主进程调用
export const APIS = {
  createProcess,
  runProcessMain,
  createIpc,
  createIpcFail,
  destroyProcess,
};
export type $RunMainConfig = {
  main_url: string;
};

Object.assign(globalThis, APIS);

const html = String.raw;
ALL_PROCESS_MAP.onChange((map) => {
  let innerHTML = "";
  for (const [process_id, processDetail] of map) {
    innerHTML += html`<div>
      <span>PID:${process_id}</span>
      <span>URL:${processDetail.env_script_url}</span>
    </div>`;
  }
  document.body.innerHTML = innerHTML;
});

window.onbeforeunload = (event) => {
  return (event.returnValue = "666");
};
