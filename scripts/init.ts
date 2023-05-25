import { ConTasks } from "./helper/ConTasks.ts";
export const initTasks = new ConTasks({
  "desktop-dev": {
    cmd: "pnpm",
    args: "install",
    cwd: "./desktop-dev",
  },
  plaoc: {
    cmd: "deno",
    args: "task init:demo",
    cwd: "./plaoc",
  },
});

if (import.meta.main) {
  initTasks.spawn();
}
