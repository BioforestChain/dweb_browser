console.log("loading tasks...");
import { toolkitTasks } from "../toolkit/scripts/toolkit-dev.ts";

import { ExitAbortController } from "./helper/ConTasks.ts";
import { doInit } from "./init.ts";
export const devTasks = toolkitTasks;

export const doDev = async (args: string[]) => {
  Deno.addSignalListener("SIGINT", () => {
    ExitAbortController.abort();
    Deno.exit();
  });

  const devArgs = [...args, "--dev"];

  // 如果有指定任务，那么默认当作是在做进阶指令，不需要做初始化
  if (devArgs.filter((it) => !it.startsWith("-")).length === 0 || devArgs.includes("--init")) {
    /// 首先取保 init 任务执行完成
    await doInit(devArgs);
  }

  /// 开始执行，强制使用开发模式进行监听
  devTasks.spawn(devArgs);
};

if (import.meta.main) {
  doDev(Deno.args);
}
