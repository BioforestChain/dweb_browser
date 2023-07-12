import { ConTasks } from "../../scripts/helper/ConTasks.ts";

/**
 * 所有的打包任务
 */
export const assetsTasks = new ConTasks(
  {
    "browser.html": {
      cmd: "npx",
      args: "vite build --outDir=../../../../electron/assets/browser/newtab --emptyOutDir -l info -c vite.config.ts",
      logLineFilter: (log) =>
        log.includes("../../../../electron/assets/browser/newtab/") === false,
      cwd: "src/browser/browser/assets",
      devAppendArgs: "--minify=false --watch",
    },
    "jmm.html": {
      cmd: "npx",
      args: "vite build src/browser/jmm/assets/ --outDir=../../../../electron/assets/browser/jmm --emptyOutDir -l info -c scripts/electron-vite.config.ts",
      logLineFilter: (log) =>
        log.includes("../../../../electron/assets/browser/jmm/") === false,
      devAppendArgs: "--minify=false --watch",
    },
    "js-process.worker": {
      cmd: "deno",
      args: "run -A ./scripts/js-process.worker.esbuild.ts",
      devAppendArgs: "--watch",
    },
    "js-process.html": {
      cmd: "npx",
      args: "vite build src/browser/js-process/assets/ --outDir=../../../../electron/assets/browser/js-process/main-thread --emptyOutDir -l info -c scripts/electron-vite.config.ts",
      logLineFilter: (log) =>
        log.includes(
          "../../../../electron/assets/browser/js-process/main-thread/"
        ) === false,
      devAppendArgs: "--minify=false --watch",
    },
    "bluetooth.html": {
      cmd: "npx",
      args: "vite build src/std/bluetooth/assets/ --outDir=../../../../electron/assets/bluetooth --emptyOutDir -l info -c scripts/electron-vite.config.ts",
      logLineFilter: (log) =>
        log.includes("../../../../electron/assets/std/bluetooth/") === false,
      devAppendArgs: "--minify=false --watch",
    },
    "barcode-scanning.html": {
      cmd: "npx",
      args: "vite build src/sys/barcode-scanning/assets/ --outDir=../../../../electron/assets/barcode-scanning --emptyOutDir -l info -c scripts/electron-vite.config.ts",
      logLineFilter: (log) =>
        log.includes("../../../../electron/assets/sys/barcode-scanning/") ===
        false,
    },
  },
  import.meta.resolve("../")
);

if (import.meta.main) {
  assetsTasks.spawn();
}
