import chalk from "npm:chalk";
import { esbuild, esbuild_deno_loader } from "../deps.ts";

export type $BuildOptions = esbuild.BuildOptions & {
  importMapURL?: string;
  signal?: AbortSignal;
};

export class ESBuild {
  constructor(readonly options: $BuildOptions) {}
  mergeOptions(...optionsList: Partial<$BuildOptions>[]) {
    const esbuildOptions = { ...this.options };
    for (const options of optionsList) {
      Object.assign(esbuildOptions, options);
    }
    esbuildOptions.plugins = [
      ...(esbuildOptions.plugins || []),
      // ESBuild plugin to rewrite import starting "npm:" to "esm.sh" for https plugin
      {
        name: "the-npm-plugin",
        setup(build: any) {
          build.onResolve({ filter: /^npm:/ }, (args: any) => {
            return {
              path: args.path.replace(/^npm:/, "//esm.sh/"),
              namespace: "https",
            };
          });
        },
      },
      ...esbuild_deno_loader.denoPlugins({
        importMapURL: this.options.importMapURL,
      }),
    ];
    for (const key of ["importMapURL", "signal"] as const) {
      delete esbuildOptions[key];
    }
    return esbuildOptions as esbuild.BuildOptions &
      Required<Pick<esbuild.BuildOptions, "plugins">>;
  }
  async build(options: Partial<$BuildOptions> = {}) {
    const result = await esbuild.build(this.mergeOptions(options));
    this._logResult(result);
    options.signal?.addEventListener("abort", () => {
      esbuild.stop();
    });
    esbuild.stop();
  }
  private _logResult(result: esbuild.BuildResult) {
    if (result.warnings) {
      for (const warning of result.warnings) {
        console.warn(chalk.red(warning.text));
      }
    }
    if (result.errors && result.errors.length > 0) {
      for (const error of result.errors) {
        console.error(chalk.yellow(error.text));
      }
    } else {
      console.log(chalk.green("[build] success ✓"));
    }
  }
  async watch(options: Partial<$BuildOptions> = {}) {
    const esbuildOptions = this.mergeOptions({ minify: false }, options);
    esbuildOptions.plugins.push({
      name: "esbuild-watch-hook",
      setup: (build) => {
        build.onEnd((result) => {
          this._logResult(result);
          console.log(
            chalk.grey(`[watch] build finished, watching for changes...`)
          );
        });
      },
    });
    const context = await esbuild.context(esbuildOptions);
    options.signal?.addEventListener("abort", async () => {
      await context.dispose();
    });

    await context.watch();
  }

  async auto() {
    if (Deno.args.includes("--watch")) {
      await this.watch();
    } else {
      await this.build();
    }
  }
}
