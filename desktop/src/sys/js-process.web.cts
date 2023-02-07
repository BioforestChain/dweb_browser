import { PromiseOut } from "../helper/PromiseOut.cjs";
import type { $RunMainConfig } from "./js-process.worker.cjs";

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
  const process_id = allocProcessId();

  const worker = new Worker(env_script_url, {});

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

  const runMain = (config: $RunMainConfig) => {
    worker.postMessage(["run-main", config]);
  };

  /// TODO 使用 weaklock 来检测线程是否唤醒

  /// 这些是 js 的对象，返回是要返回到 原生环境里
  /// 在迁移到原生 Android 时：
  /// 可以通过 evalJs 获取 process_id、或者执行 runMain
  return {
    process_id,
    runMain,
  };
};
const createIpc = (worker_id: number) => {
  const process = ALL_PROCESS_MAP.get(worker_id);
  if (process === undefined) {
    throw new Error(`no found worker by id: ${worker_id}`);
  }
  const channel = new MessageChannel();
  process.worker.postMessage(["ipc-channel", channel.port2], [channel.port2]);
  return channel.port1;
};

export const APIS = {
  // allocProcessId,
  createProcess,
  createIpc,
};
