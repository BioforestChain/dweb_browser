import { fileURLToPath } from "node:url";
import { esbuildTaskFactory, viteTaskFactory } from "../../scripts/helper/ConTasks.helper.ts";
import { ConTasks } from "../../scripts/helper/ConTasks.ts";

const baseDir = fileURLToPath(import.meta.resolve("../"));

/**
 * 所有的打包任务
 */
export const assetsTasks = new ConTasks(
  {
    desk: viteTaskFactory({
      inDir: "src/browser/desk/",
      outDir: "../next/kmp/browser/src/commonMain/composeResources/files/browser/desk",
      baseDir,
    }),
    "jmm.html": viteTaskFactory({
      inDir: "src/browser/jmm/",
      outDir: "../next/kmp/browser/src/commonMain/composeResources/files/browser/jmm",
      baseDir,
    }),
    "js-process.worker.js": esbuildTaskFactory({
      input: "src/js-process/worker/index.ts",
      outfile: "../next/kmp/browser/src/commonMain/composeResources/files/browser/js-process.worker/index.js",
      baseDir,
    }),
    "js-process.main.html": viteTaskFactory({
      inDir: "src/js-process/main",
      outDir: "../next/kmp/browser/src/commonMain/composeResources/files/browser/js-process.main",
      // viteConfig: "scripts/electron-vite.config.ts",
      baseDir,
    }),
  },
  /// base
  baseDir
);

if (import.meta.main) {
  assetsTasks.spawn();
}
