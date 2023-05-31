import { ConTasks } from "../../scripts/helper/ConTasks.ts";

/**
 * 所有的打包任务
 */
const devTasks = new ConTasks({
  // "cot-demo":{
  //   "cmd":'pnpm',
  //   args:""
  // } src/sys/dweb-browser/assets/index.html
  "dweb-browser.html": {
    cmd: "npx",
    args: "vite build src/sys/dweb-browser/assets/ --outDir=../../../../electron/assets/dweb-browser --emptyOutDir -c electron-vite.config.ts",
    devAppendArgs: "--minify=false --watch",
  },
  "jmm.html": {
    cmd: "npx",
    args: "vite build src/sys/jmm/assets/ --outDir=../../../../electron/assets/jmm --emptyOutDir -c electron-vite.config.ts",
    devAppendArgs: "--minify=false --watch",
  },
  "js-process.worker": {
    cmd: "npx",
    args: "esbuild ./src/sys/js-process/js-process.worker.ts --outfile=./electron/assets/js-process.worker.js --bundle --format=esm --target=es2020",
    devAppendArgs: "--watch",
  },
  "js-process.html": {
    cmd: "npx",
    args: "vite build src/sys/js-process/assets/ --outDir=../../../../electron/assets/js-process --emptyOutDir -c electron-vite.config.ts",
    devAppendArgs: "--minify=false --watch",
  },
  public: {
    cmd: "npx",
    args: "esbuild ./src/user/public-service/public.service.worker.ts --outfile=./electron/assets/public.service.worker.js --bundle --format=esm --target=es2020",
    devAppendArgs: "--watch",
  },
  "browser.worker": {
    cmd: "node",
    args: "./scripts/esbuild.browser.worker.js",
    devAppendArgs: "--watch",
  },
  "multi-webview.html": {
    cmd: "npx",
    args: "vite build src/sys/multi-webview/assets/ --outDir=../../../../electron/assets/multi-webview --emptyOutDir -c electron-vite.config.ts",
    devAppendArgs: "--minify=false --watch",
  },
  "desktop.worker": {
    cmd: "npx",
    args: "esbuild ./src/user/desktop/desktop.worker.ts --outfile=./electron/assets/desktop.worker.js --bundle --format=esm --target=es2020",
    devAppendArgs: "--watch",
  },
  // "desktop_2.worker": {
  //   cmd: "npx",
  //   args: "esbuild ./src/user/desktop_2/desktop.worker.ts --outfile=./electron/assets/desktop_2.worker.js --bundle --format=esm --target=es2020",
  //   devAppendArgs: "--watch",
  // },
  // "test.worker": {
  //   cmd: "npx",
  //   args: "esbuild ./src/user/test/test.worker.ts --outfile=./electron/assets/test.worker.js --bundle --format=esm --target=es2020",
  //   devAppendArgs: "--watch",
  // },
  // "toy.worker": {
  //   cmd: "npx",
  //   args: "esbuild ./src/user/toy/toy.worker.ts --outfile=./electron/assets/toy.worker.js --bundle --format=esm --target=es2020",
  //   devAppendArgs: "--watch",
  // },
  // "jmm.test.connect.worker": {
  //   cmd: "npx",
  //   args: "esbuild ./src/user/jmm-test-connect/jmmtestconnect.worker.ts --outfile=./electron/assets/jmmtestconnect.worker.js --bundle --format=esm",
  //   devAppendArgs: "--watch",
  // },
  // "jmm.test.connect2.worker": {
  //   cmd: "npx",
  //   args: "esbuild ./src/user/jmm-test-connect2/jmmtestconnect2.worker.ts --outfile=./electron/assets/jmmtestconnect2.worker.js --bundle --format=esm",
  //   devAppendArgs: "--watch",
  // },
});

if (import.meta.main) {
  devTasks.spawn();
}
