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
      outDir: "electron/assets/browser/desk",
      baseDir,
    }),
    "jmm.html": viteTaskFactory({
      inDir: "src/browser/jmm/",
      outDir: "electron/assets/browser/jmm",
      baseDir,
    }),
    "js-process.worker.js": esbuildTaskFactory({
      input: "src/browser/js-process/worker/index.ts",
      outfile: "electron/assets/browser/js-process.worker/index.js",
      baseDir,
      importMap: "src/browser/js-process/worker/import_map.json",
    }),
    "js-process.main.html": viteTaskFactory({
      inDir: "src/browser/js-process/main",
      outDir: "electron/assets/browser/js-process.main",
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
