import { fileURLToPath } from "node:url";
import { ESBuild } from "../../scripts/helper/Esbuild.ts";

export const esbuilder = new ESBuild({
  entryPoints: [fileURLToPath(import.meta.resolve("../src/server/index.ts"))],
  outfile: fileURLToPath(import.meta.resolve("../dist/server/plaoc.server.js")),
  bundle: true,
  format: "esm",
  denoLoader: true,
  importMapURL: import.meta.resolve("../import_map.json"),
});

if (import.meta.main) {
  esbuilder.auto();
}
