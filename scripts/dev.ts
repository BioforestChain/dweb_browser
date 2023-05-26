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
  "desktop-dev:browser": {
    cmd: "deno",
    args: "task build:watch:demo",
    cwd: "./desktop-dev",
  },
  "sync": { cmd: "deno", args: "task sync --watch" },
});

if (import.meta.main) {
  devTasks.spawn();
}
