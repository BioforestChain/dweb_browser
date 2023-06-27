import { ConTasks } from "./helper/ConTasks.ts";

export const initTasks = new ConTasks(
  {
    pnpm: {
      cmd: "pnpm",
      args: "install",
    },
  },
  import.meta.resolve("../")
);

if (import.meta.main) {
  initTasks.spawn();
}
