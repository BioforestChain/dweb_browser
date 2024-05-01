import fs from "node:fs";
import node_path from "node:path";
import { fileURLToPath } from "node:url";
import { Chalk } from "npm:chalk";
// import { InlineConfig, PluginOption } from "npm:vite";
import { $BuildOptions, ESBuild } from "../../../scripts/helper/ESBuild.ts";
// const minifyHTML = _minifyHTML.default();
const chalk = new Chalk({ level: 3 });

const resolveTo = (to: string) => fileURLToPath(import.meta.resolve(to));
const absWorkingDir = resolveTo("../server");

const prodEsbuildOptions = {
  absWorkingDir,
  splitting: true,
  entryPoints: {
    "plaoc.server": "./index.ts",
  },
  outdir: "./dist/prod",
  importMapURL: import.meta.resolve("../../../deno.jsonc"),
  denoLoader: true,
  chunkNames: "[name]-[hash]",
  bundle: true,
  platform: "browser",
  target: "es2022",
  format: "esm",
  tsconfigRaw: {
    compilerOptions: {
      experimentalDecorators: true,
    },
  },
} satisfies $BuildOptions;
export const prod = new ESBuild(prodEsbuildOptions);
export const dev = new ESBuild({
  ...prodEsbuildOptions,
  entryPoints: {
    "plaoc.server": "./index.ts",
  },
  outdir: "./dist/dev",
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
          const newpath = node_path.resolve(args.resolveDir, args.path.replace(/\.ts$/, ".(dev).ts"));
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
});

export const doBundleServer = (args = Deno.args) => {
  try {
    Deno.removeSync(resolveTo("../server/dist"), { recursive: true });
  } catch (e) {
    // 第一次运行不存在dist目录
    if (!(e instanceof Deno.errors.NotFound)) {
      throw e;
    }
  }
  return Promise.all([
    //
    prod.auto(args),
    dev.auto(args),
  ]);
};

if (import.meta.main) {
  doBundleServer();
}
