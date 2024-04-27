import { ConTasks } from "../../scripts/helper/ConTasks.ts";

export const pubTasks = new ConTasks(
  {
    "core:build": {
      cmd: "pnpm",
      args: "publish --access public",
      cwd: "./toolkit/dweb-core/npm",
    },
  },
  import.meta.resolve("../")
);
