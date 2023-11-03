import { fileURLToPath } from "node:url";
import { esbuildTaskFactory, viteTaskFactory } from "../../scripts/helper/ConTasks.helper.ts";
import { ConTasks } from "../../scripts/helper/ConTasks.ts";

const baseDir = fileURLToPath(import.meta.resolve("../"));

/**
 * 所有的打包任务
 */
export const assetsTasks = new ConTasks(
  {
    "web-browser.html": viteTaskFactory({
      inDir: "src/browser/web/assets",
      outDir: "electron/assets/browser/web",
      baseDir,
    }),
    desk: viteTaskFactory({
      inDir: "src/browser/desk/assets/desktop",
      outDir: "electron/assets/browser/desk",
      baseDir,
    }),
    "jmm.html": viteTaskFactory({
      inDir: "src/browser/jmm/assets",
      outDir: "electron/assets/browser/jmm",
      baseDir,
    }),
    "js-process.worker.js": esbuildTaskFactory({
      input: "src/browser/js-process/assets/worker/index.ts",
      outfile: "electron/assets/browser/js-process.worker/index.js",
      baseDir,
      importMap: "src/browser/js-process/assets/worker/import_map.json",
    }),
    "js-process.main.html": viteTaskFactory({
      inDir: "src/browser/js-process/assets/main",
      outDir: "electron/assets/browser/js-process.main",
      viteConfig: "scripts/electron-vite.config.ts",
      baseDir,
    }),
    "bluetooth.html": viteTaskFactory({
      inDir: "src/std/bluetooth/assets",
      outDir: "electron/assets/bluetooth",
      viteConfig: "scripts/electron-vite.config.ts",
      baseDir,
    }),
    "barcode-scanning.html": viteTaskFactory({
      inDir: "src/sys/barcode-scanning/assets",
      outDir: "electron/assets/barcode-scanning",
      viteConfig: "scripts/electron-vite.config.ts",
      baseDir,
    }),
  },
  /// base
  baseDir
);

if (import.meta.main) {
  assetsTasks.spawn();
}
