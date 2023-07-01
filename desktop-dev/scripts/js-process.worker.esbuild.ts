import { fileURLToPath } from "node:url";
import { ESBuild } from "../../scripts/helper/Esbuild.ts";

/**
 * 这里编译走没有使用 denoloader
 * 因为是要给browser平台使用，denoloader主要还是面向nodejs，所以不是很适用。
 * 因此这里的依赖解析主要靠 ../package.json 的 dependencies 来定义
 */
export const esbuilder = new ESBuild({
  entryPoints: [
    fileURLToPath(
      import.meta.resolve("../src/browser/js-process/js-process.worker.ts")
    ),
  ],
  outfile: fileURLToPath(
    import.meta.resolve(
      "../electron/assets/browser/js-process/worker-thread/js-process.worker.js"
    )
  ),
  bundle: true,
  format: "esm",
  target: "es2020",
  platform: "browser",
});

if (import.meta.main) {
  esbuilder.auto();
}
