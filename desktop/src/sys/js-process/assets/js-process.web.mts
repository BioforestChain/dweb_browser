import { createSignal } from "../../../helper/createSignal.mjs";
import { PromiseOut } from "../../../helper/PromiseOut.mjs";

/// 这个文件是用在 js-process.html 的主线程中直接运行的，用来协调 js-worker 与 native 之间的通讯
const ALL_PROCESS_MAP = new Map<
  number,
  {
    worker: Worker;
    fetch_port: MessagePort;
  }
>();
let acc_process_id = 0;
const allocProcessId = () => acc_process_id++;

const createProcess = async (
  env_script_url: string,
  fetch_port: MessagePort
) => {
  console.log(env_script_url, fetch_port);
  const process_id = allocProcessId();
  const worker_url = URL.createObjectURL(
    new Blob(
      [
        `import("${env_script_url}").then(()=>postMessage("ready"),(err)=>postMessage("ERROR:"+err))`,
      ],
      {
        // esm 代码必须有正确的 mime
        type: "application/javascript",
      }
    )
  );
  /// https://caniuse.com/mdn-api_worker_worker_ecmascript_modules 需要 2019 年之后的 WebView 支持： Safari 15+ || Chrome 80+
  const worker = new Worker(worker_url, { type: "module" });
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
  ALL_PROCESS_MAP.set(process_id, { worker, fetch_port });

  /// 触发事件
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

const _forceGetProcess = (process_id: number) => {
  const process = ALL_PROCESS_MAP.get(process_id);
  if (process === undefined) {
    throw new Error(`no found worker by id: ${process_id}`);
  }
  return process;
};
const runProcessMain = (process_id: number, config: $RunMainConfig) => {
  const process = _forceGetProcess(process_id);debugger
  process.worker.postMessage(["run-main", config]);
};

const createIpc = (process_id: number) => {
  const process = _forceGetProcess(process_id);
  const channel = new MessageChannel();
  process.worker.postMessage(["ipc-channel", channel.port2], [channel.port2]);
  return channel.port1;
};

const on_create_process_signal = createSignal();

export const APIS = {
  createProcess,
  runProcessMain,
  createIpc,
};
export type $RunMainConfig = {
  main_url: string;
};

Object.assign(globalThis, APIS);

const html = String.raw;
on_create_process_signal.listen(({ process_id, env_script_url }) => {
  document.body.innerHTML += html`<div>
    <span>PID:${process_id}</span>
    <span>URL:${env_script_url}</span>
  </div>`;
});
