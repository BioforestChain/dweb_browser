import { ConTasks } from "./helper/ConTasks.ts";
export const devTasks = new ConTasks({
  assets: {
    cmd: "deno",
    args: "task assets --dev",
    cwd: "./desktop-dev",
  },
  "plaoc:src": {
    cmd: "deno",
    args: "task dev:src",
    cwd: "./plaoc",
  },
  "plaoc:demo": {
    cmd: "deno",
    args: "task build:watch:demo",
    cwd: "./plaoc",
    startDeps: [
      {
        name: "plaoc:src",
        whenLog: "Process finished.",
        logType: "stderr",
      },
    ],
  },
  "sync": { cmd: "deno", args: "task sync --watch" },
});

if (import.meta.main) {
  devTasks.spawn();
}
