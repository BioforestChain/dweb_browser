console.log("loading tasks...");
import { toolkitTasks } from "../toolkit/scripts/toolkit-dev.ts";

import { ConTasks, ExitAbortController } from "./helper/ConTasks.ts";
import { doInit } from "./init.ts";
export const devTasks = new ConTasks(
  {
    "plaoc:server": {
      cmd: "deno",
      args: "task bundle:watch:server",
      cwd: "./toolkit/plaoc",
    },
    "plaoc:demo": {
      cmd: "deno",
      args: "task build:watch:demo",
      cwd: "./toolkit/plaoc",
      startDeps: [{ name: "plaoc:demo", whenLog: "Complete!", logType: "stdout" }],
    },
    "plaoc:client": {
      cmd: "deno",
      args: "task build:client",
      devArgs: "task build:client:watch",
      cwd: "./toolkit/plaoc",
    },
    "plaoc:is-dweb": {
      cmd: "deno",
      args: "task build:is-dweb",
      cwd: "./toolkit/plaoc",
    },
    sync: {
      cmd: "deno",
      args: "task sync --watch",
    },
  },
  import.meta.resolve("../")
)
  .merge(toolkitTasks, "toolkit:");

if (import.meta.main) {
  Deno.addSignalListener("SIGINT", () => {
    ExitAbortController.abort();
    Deno.exit();
  });

  // 如果有指定任务，那么默认当作是在做进阶指令，不需要做初始化
  if (Deno.args.length === 0 || Deno.args.includes("--init")) {
    /// 首先取保 init 任务执行完成
    await doInit();
  }

  /// 开始执行，强制使用开发模式进行监听
  devTasks.spawn([...Deno.args, "--dev"]);
}
