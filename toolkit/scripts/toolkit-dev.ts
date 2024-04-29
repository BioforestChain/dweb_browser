import { esbuildTaskFactory } from "../../scripts/helper/ConTasks.helper.ts";
import { ConTasks } from "../../scripts/helper/ConTasks.ts";

export const toolkitTasks = new ConTasks(
  {
    "fort-test-image:serve": {
      cmd: "npx",
      args: "vite --host 0.0.0.0",
      cwd: "../for-test-images",
    },
    "dweb-offscreen-web-canvas-assets": {
      cmd: "npx",
      args: [
        `vite`,
        `build`,
        `--outDir`,
        `../../../next/kmp/pureImage/src/commonMain/composeResources/files/offscreen-web-canvas`,
      ],
      devAppendArgs: ["--watch"],
      cwd: "../dweb-offscreen-web-canvas-assets",
    },
    "dweb-polyfill": {
      cmd: "npx",
      args: [`vite`, `build`],
      devAppendArgs: ["--watch"],
      cwd: "../dweb-polyfill",
    },
    "dweb-desk-assets": {
      cmd: "npx",
      args: [
        "vite",
        "build",
        "--emptyOutDir",
        "--outDir",
        "../../next/kmp/browser/src/commonMain/composeResources/files/browser/desk",
      ],
      devAppendArgs: ["--watch"],
      cwd: "../dweb-desk-assets",
    },
    "js-process.worker.js": esbuildTaskFactory({
      input: "../dweb-js-process-assets/index.ts",
      outfile: "../../next/kmp/browser/src/commonMain/composeResources/files/browser/js-process.worker/index.js",
      baseDir: import.meta.resolve("./"),
      importMap: import.meta.resolve("../../deno.jsonc"),
      tsconfig: {
        compilerOptions: {
          experimentalDecorators: true,
        },
      },
    }),
    "js-process.main.html": {
      cmd: "deno",
      args: [
        "run",
        "-A",
        "npm:vite",
        "build",
        "--emptyOutDir",
        "--outDir",
        "../../../next/kmp/browser/src/commonMain/composeResources/files/browser/js-process.main",
      ],
      devAppendArgs: ["--watch"],
      cwd: "../dweb-js-process-assets/main",
    },
  },
  import.meta.resolve("./")
);

if (import.meta.main) {
  toolkitTasks.spawn(Deno.args);
}
