import { ConTasks } from "./helper/ConTasks.ts";

export const buildTasks = new ConTasks(
  {
    build: {
      cmd: "deno",
      args: "run -A ./toolkit/scripts/build_npm.ts",
    },
  },
  import.meta.resolve("../")
);
if (import.meta.main) {
  buildTasks.spawn();
}
