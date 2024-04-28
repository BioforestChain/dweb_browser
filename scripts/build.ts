import { ConTasks } from "./helper/ConTasks.ts";

export const pubTasks = new ConTasks(
  {
    pub: {
      cmd: "deno",
      args: "run -A ./toolkit/scripts/build_npm.ts",
    },
  },
  import.meta.resolve("../")
);
if (import.meta.main) {
  pubTasks.spawn();
}
