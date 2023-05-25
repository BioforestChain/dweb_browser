import { ConTasks } from "./helper/ConTasks.ts";
export const initTasks = new ConTasks({
  pnpm: {
    cmd: "pnpm",
    args: "install",
  },
});

if (import.meta.main) {
  initTasks.spawn();
}
