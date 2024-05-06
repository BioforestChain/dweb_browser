import { ConTasks } from "./helper/ConTasks.ts";

export const pubTasks = new ConTasks(
  {
    pub: {
      cmd: "pnpm",
      args: "publish --access public --no-git-checks -r",
      devAppendArgs: "--dry-run",
    },
  },
  import.meta.resolve("../")
);
if (import.meta.main) {
  pubTasks.spawn(Deno.args);
}
