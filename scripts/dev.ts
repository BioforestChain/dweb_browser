import { ConTasks } from "./helper/ConTasks.ts";
export const devTasks = new ConTasks({
  assets: {
    cmd: "deno",
    args: "task assets --dev",
    cwd: "./desktop-dev",
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
});

import { initTasks } from "./init.ts";

if (import.meta.main) {
  await initTasks.spawn().afterComplete();
  devTasks.spawn();
}
