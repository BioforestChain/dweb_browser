import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { build, InlineConfig, PluginOption } from "npm:vite";
import { ESBuild } from "../../scripts/helper/Esbuild.ts";

const resolveTo = (to: string) => fileURLToPath(import.meta.resolve(to));
const absWorkingDir = resolveTo("../");
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
export const dev = new ESBuild({
  absWorkingDir,
  entryPoints: ["src/server/index.ts"],
  outfile: "dist/server/plaoc.server.dev.js",
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
          const newpath = path.resolve(
            args.resolveDir,
            args.path.replace(/\.ts$/, ".(dev).ts")
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

console.log("prod.isDev", prod.isDev);
export const emulator = {
  configFile: false,
  base: "./",
  root: resolveTo("../src/emulator"),
  build: {
    outDir: resolveTo("../dist/server/emulator"),
    watch: prod.isDev ? {} : undefined, // {},
    rollupOptions: {},
    emptyOutDir: true,
  },
  plugins: [
    (() => {
      const esbuilderMap = new Map<
        string,
        {
          gen: ReturnType<ESBuild["Auto"]>;
          // lastBuildItem: Promise<IteratorResult<$ESBuildWatchYield>>;
        }
      >();
      return {
        enforce: "pre",
        name: "esbuild-deno",
        async load(id) {
          if (id.endsWith(".ts")) {
            //#region 根据入口文件，构建 esbuild
            let esbuildCtx = esbuilderMap.get(id);
            if (esbuildCtx === undefined) {
              const builder = new ESBuild({
                absWorkingDir,
                entryPoints: [id],
                write: false,
                bundle: true,
                format: "esm",
                denoLoader: true,
                importMapURL,
                metafile: true,
              });
              const gen = builder.Auto();
              esbuildCtx = {
                gen,
              };
              esbuilderMap.set(id, esbuildCtx);
            }
            //#endregion

            /// 等待最后一次编译结果
            console.log("waitting", id);
            const buildItem = await esbuildCtx.gen.next();
            if (buildItem.done) {
              throw new Error(`esbuild task "${id}" already exited`);
            }

            const res = await buildItem.value.result;
            for (const inputfilepath of Object.keys(
              res.metafile?.inputs ?? {}
            )) {
              if (inputfilepath.startsWith("https://")) {
                continue;
              }
              const inputfilepath_full = path.resolve(
                absWorkingDir,
                inputfilepath
              );
              this.addWatchFile(inputfilepath_full);
            }
            if (res.errors.length) {
              for (const error of res.errors) {
                this.error(error.text);
              }
              return "";
            }
            return {
              code: res.outputFiles?.[0]?.text ?? "",
            };
          }
        },
      } satisfies PluginOption;
    })(),
  ],
} satisfies InlineConfig;

if (import.meta.main) {
  void prod.auto();
  void dev.auto();
  void build(emulator);
}
