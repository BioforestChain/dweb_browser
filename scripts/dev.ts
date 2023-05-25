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
  "sync:android": { cmd: "deno", args: "task sync:android --watch" },
  "sync:desktop": { cmd: "deno", args: "task sync:desktop --watch" },
  "sync:next": { cmd: "deno", args: "task sync:next --watch" },
});

if (import.meta.main) {
  devTasks.spawn();
}
