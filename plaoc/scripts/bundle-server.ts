import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Chalk } from "npm:chalk";
// import { InlineConfig, PluginOption } from "npm:vite";
import { ESBuild } from "../../scripts/helper/ESBuild.ts";
// const minifyHTML = _minifyHTML.default();
const chalk = new Chalk({ level: 3 });

const resolveTo = (to: string) => fileURLToPath(import.meta.resolve(to));
const absWorkingDir = resolveTo("../src/server");
console.log("absWorkingDir", absWorkingDir);
export const prod = new ESBuild({
  absWorkingDir,
  splitting: true,
  entryPoints: {
    "plaoc.server": "./index.ts",
    "urlpattern.polyfill": "./helper/urlpattern.polyfill.ts",
  }, //[, "src/server/helper/urlpattern.polyfill.ts"],
  // outfile: "../../dist/server/plaoc.server.js",
  outdir: "../../dist/server",
  chunkNames: "[name]",
  bundle: true,
  platform: "browser",
  format: "esm",
  denoLoader:true,
});
export const dev = new ESBuild({
  absWorkingDir,
  entryPoints: {
    "plaoc.server.dev": "./index.ts",
    "urlpattern.polyfill": "./helper/urlpattern.polyfill.ts",
  },
  outdir: "../../dist/server",
  plugins: [
    {
      name: "use-(dev)-ext",
      setup(build) {
        build.onResolve({ filter: /\.ts$/ }, (args) => {
          if (
            /// 必须要求在ts文件中 引用的模块
            args.importer.endsWith(".ts") === false ||
            /// 如果是 .(dev).ts 文件本身，不触发这个插件，否则会循环依赖
            args.importer.endsWith(".(dev).ts")
          ) {
            return;
          }
          const newpath = path.resolve(args.resolveDir, args.path.replace(/\.ts$/, ".(dev).ts"));
          if (fs.existsSync(newpath)) {
            console.log(chalk.gray("replace"), newpath);
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
  platform: "browser",
  format: "esm",
  denoLoader:true,
});

if (import.meta.main) {
  void prod.auto();
  void dev.auto();
}
