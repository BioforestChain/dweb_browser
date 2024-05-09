import fs from "node:fs";
import node_path from "node:path";
import { Chalk } from "npm:chalk";
import { createBaseResolveTo } from "../../../scripts/helper/ConTasks.helper.ts";
import { $BuildOptions, ESBuild } from "../../../scripts/helper/ESBuild.ts";
import { npmNameToFolder } from "../../../scripts/helper/npmBuilder.ts";
const chalk = new Chalk({ level: 3 });

const resolveTo = createBaseResolveTo(import.meta.resolve("../server"));
const absWorkingDir = resolveTo();
const serverPackageJson = await import("../server/package.json", { with: { type: "json" } }).then((res) => res.default);
const npmDir = npmNameToFolder(serverPackageJson.name);
const prodEsbuildOptions = {
  absWorkingDir,
  splitting: true,
  entryPoints: {
    "plaoc.server": "./index.ts",
  },
  outdir: node_path.resolve(npmDir, "./dist/prod"),
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
  outdir: node_path.resolve(npmDir, "./dist/dev"),
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
    Deno.removeSync(node_path.resolve(npmDir, "./dist/"), { recursive: true });
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
