import { PromiseOut } from "@dweb-browser/helper/PromiseOut.ts";
import { ReadableStreamOut, streamRead } from "@dweb-browser/helper/stream/readableStreamHelper.ts";
import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import picocolors from "npm:picocolors";
import { esbuild, esbuild_deno_loader } from "../deps.ts";
export type $BuildOptions = esbuild.BuildOptions & {
  denoLoader?: boolean;
  importMapURL?: string;
  signal?: AbortSignal;
};

export class ESBuild {
  /**
   * 所有运行中的实例，目的是知道什么时候调用 stop 函数：
   *
   * Unlike node, Deno lacks the necessary APIs to clean up child processes
   * automatically. You must manually call stop() in Deno when you're done
   * using esbuild or Deno will continue running forever.
   */
  static _runnings = new Set<ESBuild>();
  static start(origin: ESBuild) {
    this._runnings.add(origin);
  }
  static dispose(origin: ESBuild) {
    this._runnings.delete(origin);
    if (this._runnings.size === 0) {
      esbuild.stop();
    }
  }

  constructor(readonly options: $BuildOptions) {}
  mergeOptions(...optionsList: Partial<$BuildOptions>[]) {
    const esbuildOptions = { ...this.options };
    for (const options of optionsList) {
      Object.assign(esbuildOptions, options);
    }
    const plugins = (esbuildOptions.plugins ??= []);
    if (esbuildOptions.denoLoader) {
      let importMapURL = this.options.importMapURL;
      // importMapURL = `data:application/json,${JSON.stringify(importMapURL)}`;
      if (importMapURL !== undefined && !importMapURL.startsWith("file:///")) {
        importMapURL = "file:///" + importMapURL;
      }
      if (importMapURL?.endsWith(".jsonc")) {
        const importMapJsonStr = fs.readFileSync(fileURLToPath(importMapURL), "utf-8");
        const hash = crypto.createHash("sha256").update(importMapURL).digest("hex");
        const tmpJsonFile = path.join(os.tmpdir(), `import_map.tmp-${hash.slice(0, 6)}.json`);
        const importMapJson = Function(`return (${importMapJsonStr})`)();
        for (const [key, value] of Object.entries(importMapJson.imports as Record<string, string>)) {
          if (value.startsWith("./")) {
            importMapJson.imports[key] =
              path.resolve(fileURLToPath(importMapURL), "../", value) + (key.endsWith("/") ? "/" : "");
          }
        }
        fs.writeFileSync(tmpJsonFile, JSON.stringify({ imports: importMapJson.imports }, null, 2));
        importMapURL = pathToFileURL(tmpJsonFile).href;
        console.log("importMapURL", importMapURL);
      }
      plugins.push(
        {
          name: "the-npm-plugin",
          setup(build) {
            build.onResolve({ filter: /^npm:/ }, (args) => {
              return {
                path: args.path.replace(/^npm:/, "//esm.sh/"),
                namespace: "https",
              };
            });
            // 适配window文件不对
            build.onResolve({ filter: /^[a-zA-Z]:.*$/ }, (args) => {
              const code = args.path.replaceAll("\\", "/");
              return {
                path: code,
                namespace: "file",
              };
            });
          },
        },
        ...esbuild_deno_loader.denoPlugins({
          importMapURL,
        })
      );
    }

    for (const key of ["importMapURL", "signal", "denoLoader"] as const) {
      delete esbuildOptions[key];
    }
    return esbuildOptions as esbuild.BuildOptions & Required<Pick<esbuild.BuildOptions, "plugins">>;
  }

  async build(options: Partial<$BuildOptions> = {}) {
    ESBuild.start(this);
    const result = await esbuild.build(this.mergeOptions(options));
    options.signal?.addEventListener("abort", () => {
      ESBuild.dispose(this);
    });

    this._logResult(result);
    ESBuild.dispose(this);
    return result;
  }
  private _logResult(result: esbuild.BuildResult) {
    if (result.warnings) {
      for (const warning of result.warnings) {
        console.warn(picocolors.red(warning.text));
      }
    }
    if (result.errors && result.errors.length > 0) {
      for (const error of result.errors) {
        console.error(picocolors.yellow(error.text));
      }
    } else {
      console.log(picocolors.green("[build] success ✓"));
    }
  }
  Watch(options: Partial<$BuildOptions> = {}) {
    const results = new ReadableStreamOut<$ESBuildWatchYield>();
    void (async () => {
      const esbuildOptions = this.mergeOptions({ minify: false }, options);
      esbuildOptions.plugins.push({
        name: "esbuild-watch-hook",
        setup: (build) => {
          let curBuildTask: undefined | PromiseOut<esbuild.BuildResult>;
          build.onStart(() => {
            const preBuildTask = curBuildTask;
            curBuildTask = new PromiseOut<esbuild.BuildResult>();
            if (preBuildTask) {
              preBuildTask.resolve(curBuildTask.promise);
            }
            /// 在开始编译的时候就注入
            results.controller.enqueue({ result: curBuildTask.promise });
          });
          build.onEnd((result) => {
            this._logResult(result);
            console.log(picocolors.gray(`[watch] build finished, watching for changes...`));
            if (curBuildTask === undefined) {
              results.controller.error(new Error("no found task waitter"));
            } else {
              curBuildTask.resolve(result);
              curBuildTask = undefined;
            }
          });
        },
      });
      const context = await esbuild.context(esbuildOptions);
      ESBuild.start(this);
      options.signal?.addEventListener("abort", async (reason) => {
        await context.dispose();
        ESBuild.dispose(this);
        results.controller.error(reason);
      });

      await context.watch();
    })();
    return streamRead(results.stream);
  }
  async watch(options?: Partial<$BuildOptions>) {
    for await (const _ of this.Watch(options)) {
      //
    }
  }

  async *Auto() {
    if (this.isDev) {
      yield* this.Watch();
    } else {
      yield { result: this.build() };
    }
  }

  async auto() {
    for await (const _ of this.Auto()) {
      //
    }
  }
  get isDev() {
    return Deno.args.includes("--watch");
  }
}
export type $ESBuildWatchYield = {
  /// 包裹一层 result，目的是提供 start/end 的这种模式，如果只给promise，那么缺省的 resolver 会导致只能在监听到 end 的时候
  result: Promise<esbuild.BuildResult>;
};

if (import.meta.main) {
  const path = await import("node:path");
  const { Flags } = await import("../deps.ts");
  const args = Flags.parse(Deno.args, {
    collect: ["input"],
    string: ["outfile", "importMap"],
    default: { outfile: "index.js" },
  });
  if (args.input.length === 0) {
    throw new Error("esbuild require `--input` argument");
  }

  const cwd = Deno.cwd();

  const esbuilder = new ESBuild({
    entryPoints: args.input.map((input) => path.resolve(cwd, input as string)),
    outfile: path.resolve(cwd, args.outfile!),
    bundle: true,
    format: "esm",
    target: "es2020",
    platform: "browser",
    denoLoader: args.importMap !== undefined,
    importMapURL: args.importMap ? path.resolve(cwd, args.importMap) : undefined,
  });
  await esbuilder.auto();
}
