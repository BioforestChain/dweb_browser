import { fileURLToPath } from "node:url";
import { ESBuild } from "../../scripts/helper/Esbuild.ts";

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
  importMapURL: import.meta.resolve("../import_map.json"),
});

if (import.meta.main) {
  esbuilder.auto();
}
