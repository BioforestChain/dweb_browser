import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { ESBuild } from "../../scripts/helper/Esbuild.ts";

const absWorkingDir = fileURLToPath(import.meta.resolve("../"));
const importMapURL = import.meta.resolve("../import_map.json");

export const prod = new ESBuild({
  absWorkingDir,
  entryPoints: ["src/server/index.ts"],
  outfile: "dist/server/plaoc.server.js",
  bundle: true,
  format: "esm",
  denoLoader: true,
  importMapURL,
});
export const emulator = new ESBuild({
  absWorkingDir,
  entryPoints: ["src/emulator/index.ts"],
  outfile: "dist/server/plaoc.emulator.js",
  bundle: true,
  format: "esm",
  denoLoader: true,
  importMapURL,
});
export const dev = new ESBuild({
  absWorkingDir,
  entryPoints: ["src/server/index.ts"],
  outfile: "dist/server/plaoc.server.dev.js",
  plugins: [
    {
      name: "use-#dev-ext",
      setup(build) {
        build.onResolve({ filter: /\.ts$/ }, (args) => {
          if (
            /// 必须要求在ts文件中 引用的模块
            args.importer.endsWith(".ts") === false ||
            /// 如果是 .#dev.ts 文件本身，不触发这个插件，否则会循环依赖
            args.importer.endsWith(".#dev.ts")
          ) {
            return;
          }
          const newpath = path.resolve(
            args.resolveDir,
            args.path.replace(/\.ts$/, ".#dev.ts")
          );
          if (fs.existsSync(newpath)) {
            console.log("newpath", newpath);
            return {
              path: newpath,
              watchFiles: [newpath],
            };
          }
        });
      },
    },
  ],
  bundle: true,
  format: "esm",
  denoLoader: true,
  importMapURL,
});

if (import.meta.main) {
  prod.auto();
  emulator.auto();
  dev.auto();
}
