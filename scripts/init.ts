import { ConTasks } from "./helper/ConTasks.ts";
import {
  which,
} from "https://deno.land/x/which/mod.ts";

// 为了适配windows平台
const pathToPnpm = await which("pnpm");
export const initTasks = new ConTasks(
  {
    pnpm: {
      cmd: pathToPnpm!,
      args: "install",
    },
  },
  import.meta.resolve("../")
);

if (import.meta.main) {
  initTasks.spawn();
}
