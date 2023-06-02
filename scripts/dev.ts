import { assetsTasks } from "../desktop-dev/scripts/assets.ts";
import { ConTasks } from "./helper/ConTasks.ts";
export const devTasks = new ConTasks(
  {
    "plaoc:server": {
      cmd: "deno",
      args: "task build:watch:server",
      cwd: "./plaoc",
    },
    "plaoc:demo": {
      cmd: "deno",
      args: "task build:watch:demo",
      cwd: "./plaoc",
    },
    sync: {
      cmd: "deno",
      args: "task sync --watch",
    },
  },
  import.meta.resolve("../")
).merge(assetsTasks, "assets:");

import { initTasks } from "./init.ts";

if (import.meta.main) {
  /// 首先取保 init 任务执行完成
  await initTasks.spawn([]).afterComplete();
  /// 开始执行，强制使用开发模式进行监听
  devTasks.spawn([...Deno.args, "--dev"]);
}
