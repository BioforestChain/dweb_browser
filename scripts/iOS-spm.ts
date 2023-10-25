import { ConTasks } from "./helper/ConTasks.ts";

export const spmTasks = new ConTasks(
  {
    "spm-resolve":{
      cmd: "swift",
      args: "package resolve",
      cwd: "../next/kmp/app/iosApp",
      os: "darwin"
    },
  },
  import.meta.resolve("./")
);

if(Deno.build.os === "darwin" && import.meta.main) {
  spmTasks.spawn(Deno.args);
}
