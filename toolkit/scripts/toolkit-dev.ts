import { AssetsConfig, UseAssets } from "../../scripts/helper/AssetsConfig.ts";
import { esbuildTaskFactory } from "../../scripts/helper/ConTasks.helper.ts";
import { ConTasks } from "../../scripts/helper/ConTasks.ts";

const defineAssets = (assetsName: string, ...useAssets: UseAssets[]) => {
  return AssetsConfig.createAndSave(assetsName, useAssets).assetsDirname;
};
export const toolkitTasks = new ConTasks(
  {
    "fort-test:serve": {
      cmd: "npx",
      args: "vite --host 0.0.0.0",
      cwd: "../for-test",
    },
    "dweb-offscreen-web-canvas-assets": {
      cmd: "npx",
      args: [
        `vite`,
        `build`,
        `--outDir`,
        defineAssets(`offscreen-web-canvas`, { type: "linkKmpResFiles", moduleName: "pureImage" }),
        // `../../../next/kmp/pureImage/src/commonMain/composeResources/files/offscreen-web-canvas`,
      ],
      devAppendArgs: ["--watch"],
      cwd: "../dweb-offscreen-web-canvas-assets",
    },
    "dweb-polyfill": {
      cmd: "npx",
      args: [
        `vite`,
        `build`,
        `--outDir`,
        defineAssets(`dwebview-polyfill`, { type: "linkKmpResFiles", moduleName: "dwebview" }),
        // `../../next/kmp/dwebview/src/commonMain/composeResources/files/dwebview-polyfill`,
      ],
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
        defineAssets(`browser-desk`, { type: "linkKmpResFiles", moduleName: "browser" }),
        // "../../next/kmp/browser/src/commonMain/composeResources/files/browser/desk",
        // TODO
        // "--log-cwd",
        // "../",
      ],
      devAppendArgs: ["--watch"],
      cwd: "../dweb-desk-assets",
    },
    "js-process.worker.js": esbuildTaskFactory({
      input: "../dweb-js-process-assets/index.ts",
      outfile:
        defineAssets("browser-js-process-worker", { type: "linkKmpResFiles", moduleName: "browser" }) + "/index.js",
      // "../../next/kmp/browser/src/commonMain/composeResources/files/browser/js-process.worker/index.js",
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
        defineAssets("browser-js-process-main", { type: "linkKmpResFiles", moduleName: "browser" }),
        // "../../../next/kmp/browser/src/commonMain/composeResources/files/browser/js-process.main",
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
