import { ConTasks } from "./helper/ConTasks.ts";
export const devTasks = new ConTasks({
  "sys:*": {
    cmd: "deno",
    args: "task bundle --dev",
    cwd: "./desktop-dev",
  },
  // "sys:src": {
  //   cmd: "deno",
  //   args: "task dev:src",
  //   cwd: "./plaoc",
  // },
  "sys:demo": {
    cmd: "deno",
    args: "task build:watch:demo",
    cwd: "./plaoc",
  },
  "sync:android": { cmd: "deno", args: "task sync:android --watch" },
  "sync:desktop": { cmd: "deno", args: "task sync:desktop --watch" },
  "sync:next": { cmd: "deno", args: "task sync:next --watch" },
});

if (import.meta.main) {
  devTasks.spawn();
}
