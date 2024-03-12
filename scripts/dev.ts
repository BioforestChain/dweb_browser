console.log("loading tasks...");
import { assetsTasks } from "../desktop-dev/scripts/assets-tasks.ts";
import { toolkitTasks } from "../toolkit/toolkit-dev.ts";

import { ConTasks, ExitAbortController } from "./helper/ConTasks.ts";
export const devTasks = new ConTasks(
  {
    "plaoc:server": {
      cmd: "deno",
      args: "task bundle:watch:server",
      cwd: "./plaoc",
    },
    "plaoc:demo": {
      cmd: "deno",
      args: "task build:watch:demo",
      cwd: "./plaoc",
      startDeps: [{ name: "plaoc:demo", whenLog: "Complete!", logType: "stdout" }],
    },
    "plaoc:client": {
      cmd: "deno",
      args: "task build:client",
      devArgs: "task build:client:watch",
      cwd: "./plaoc",
    },
    "plaoc:is-dweb": {
      cmd: "deno",
      args: "task build:is-dweb",
      cwd: "./plaoc",
    },
    sync: {
      cmd: "deno",
      args: "task sync --watch",
    },
  },
  import.meta.resolve("../")
)
  .merge(assetsTasks, "assets:")
  .merge(toolkitTasks, "toolkit:");

if (import.meta.main) {
  Deno.addSignalListener("SIGINT", () => {
    ExitAbortController.abort();
    Deno.exit();
  });

  // /// 首先取保 init 任务执行完成
  // await initTasks.spawn([]).afterComplete();
  /// 开始执行，强制使用开发模式进行监听
  devTasks.spawn([...Deno.args, "--dev"]);
}
