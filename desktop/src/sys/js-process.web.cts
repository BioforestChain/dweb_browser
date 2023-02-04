import { dataUrlToUtf8, PromiseOut } from "../core/helper.cjs";
import { $IpcOnMessage, IPC_ROLE } from "../core/ipc.cjs";
import { NativeIpc } from "../core/ipc.native.cjs";
import type { MicroModule } from "../core/micro-module.cjs";
const JS_PROCESS_WORKER_CODE = fetch(
  new URL("bundle/js-process.worker.cjs", location.href)
).then(async (res) => {
  const prepare_code = await res.text();
  const install_code = wrapCommonJsCode(prepare_code, {
    after: ".installEnv()",
  });

  return install_code;

  // const blob = new Blob([install_code], { type: "application/javascript" });
  // const blob_url = URL.createObjectURL(blob);
  // return `importScripts("${blob_url}");`;
});

const wrapCommonJsCode = (
  common_js_code: string,
  options: {
    before?: string;
    after?: string;
  } = {}
) => {
  const { before = "", after = "" } = options;

  return `${before};((module,exports=module.exports)=>{${common_js_code.replaceAll(
    `"use strict";`,
    ""
  )};return module.exports})({exports:{}})${after};`;
};

/// 这个文件是用在 js-process.html 的主线程中直接运行的，用来协调 js-worker 与 native 之间的通讯
const ALL_PROCESS_MAP = new Map<number, { worker: Worker; ipc: NativeIpc }>();
let acc_process_id = 0;
const createProcess = async (
  module: MicroModule,
  main_code: string,
  onMessage: $IpcOnMessage
) => {
  const process_id = acc_process_id++;
  const prepare_code = await JS_PROCESS_WORKER_CODE;
  const worker_code = wrapCommonJsCode(main_code, { before: prepare_code });

  const data_url = dataUrlToUtf8(worker_code, false, "application/javascript");

  const worker = new Worker(data_url);

  /// 一些启动任务
  const ipc_port_po = new PromiseOut<MessagePort>();
  const onIpcChannelConnected = (event: MessageEvent) => {
    if (Array.isArray(event.data) && event.data[0] === "fetch-ipc-channel") {
      ipc_port_po.resolve(event.data[1]);
    }
  };

  /// 等待启动任务完成
  worker.addEventListener("message", onIpcChannelConnected);
  const ipc = new NativeIpc(await ipc_port_po.promise, module, IPC_ROLE.CLIENT);
  worker.removeEventListener("message", onIpcChannelConnected);

  /// 保存 js-process
  ALL_PROCESS_MAP.set(process_id, { worker, ipc });

  /// 绑定监听
  ipc.onMessage(onMessage);

  /// TODO 使用 weaklock 来检测线程是否唤醒
  return process_id;
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
  createProcess,
  createIpc,
};
