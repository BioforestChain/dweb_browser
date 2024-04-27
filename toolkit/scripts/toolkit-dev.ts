import { ConTasks } from "../../scripts/helper/ConTasks.ts";

export const toolkitTasks = new ConTasks(
  {
    "fort-test-image:serve": {
      cmd: "npx",
      args: "vite --host 0.0.0.0",
      cwd: "./for-test-images",
    },
    "offscreen-web-canvas:build": {
      cmd: "npx",
      args: [
        `vite`,
        `build`,
        `--outDir`,
        `../../../next/kmp/pureImage/src/commonMain/composeResources/files/offscreen-web-canvas`,
      ],
      devAppendArgs: ["--watch"],
      cwd: "./offscreen-web-canvas",
    },
    "dwebview-polyfill": {
      cmd: "npx",
      args: [`vite`, `build`],
      devAppendArgs: ["--watch"],
      cwd: "./dwebview-polyfill",
    },
  },
  import.meta.resolve("./")
);

if (import.meta.main) {
  toolkitTasks.spawn(Deno.args);
}
