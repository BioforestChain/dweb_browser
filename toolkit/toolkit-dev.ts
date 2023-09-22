import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createBaseResolveTo } from "../scripts/helper/ConTasks.helper.ts";
import { ConTasks } from "../scripts/helper/ConTasks.ts";
import { whichSync } from "../scripts/helper/WhichCommand.ts";
const $ = async (cmd: string | string[], cwd?: string) => {
  if (typeof cmd === "string") {
    cmd = cmd.split(/\s+/);
  }
  const [exec, ...args] = cmd;
  const cmdWhich = whichSync(exec);
  const command = new Deno.Command(cmdWhich!, { args, cwd: cwd && resolveTo(cwd) });
  await command.output();
};
const resolveTo = createBaseResolveTo(path.dirname(fileURLToPath(import.meta.url)));

/// 初始化一些环境
if (!fs.existsSync(resolveTo("./offscreen-web-canvas"))) {
  await $(`git submodule init`, "../");
}
/// 拉取更新
await $(
  `git submodule foreach git pull origi
n main`,
  "../"
);
/// 安装依赖
await $(`pnpm install`, "./offscreen-web-canvas");

export const toolkitTasks = new ConTasks(
  {
    "fort-test-image:serve":{
      cmd: "npx",
      args: "vite --host 0.0.0.0",
      cwd: "./for-test-images",
    },
    "offscreen-web-canvas:build": {
      cmd: "npx",
      args: [
        `vite`,
        `build`,
        `--watch`,
        `--outDir`,
        `../../../next/kmp/shared/src/commonMain/resources/offscreen-web-canvas`,
      ],
      devAppendArgs: ["--watch"],
      cwd: "./offscreen-web-canvas",
    },
  },
  import.meta.resolve("./")
);

if (import.meta.main) {
  toolkitTasks.spawn(Deno.args);
}
