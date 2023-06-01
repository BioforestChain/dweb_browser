import { ConTasks } from "../../scripts/helper/ConTasks.ts";

/**
 * 所有的打包任务
 */
export const assetsTasks = new ConTasks({
  "browser.html": {
    cmd: "npx",
    args: "vite build --outDir=../../../../electron/assets/browser/newtab --emptyOutDir -c vite.config.ts",
    cwd: "src/browser/browser/assets",
    devAppendArgs: "--minify=false --watch",
  },
  "jmm.html": {
    cmd: "npx",
    args: "vite build src/browser/jmm/assets/ --outDir=../../../../electron/assets/browser/jmm --emptyOutDir -c scripts/electron-vite.config.ts",
    devAppendArgs: "--minify=false --watch",
  },
  "js-process.worker": {
    cmd: "deno",
    args: "run -A ./scripts/js-process.worker.esbuild.ts",
    devAppendArgs: "--watch",
  },
  "js-process.html": {
    cmd: "npx",
    args: "vite build src/browser/js-process/assets/ --outDir=../../../../electron/assets/browser/js-process/main-thread --emptyOutDir -c scripts/electron-vite.config.ts",
    devAppendArgs: "--minify=false --watch",
  },
  "multi-webview.html": {
    cmd: "npx",
    args: "vite build src/browser/multi-webview/assets/ --outDir=../../../../electron/assets/browser/multi-webview --emptyOutDir -c scripts/electron-vite.config.ts",
    devAppendArgs: "--minify=false --watch",
  },
});

if (import.meta.main) {
  assetsTasks.spawn();
}
