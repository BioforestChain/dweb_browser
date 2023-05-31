import * as esbuild from "https://deno.land/x/esbuild@v0.17.19/mod.js";
import * as esbuild_deno_loader from "https://deno.land/x/esbuild_deno_loader@0.7.0/mod.ts";

export const options: esbuild.BuildOptions = {
  plugins: [
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
      importMapURL: import.meta.resolve("../import_map.json"),
    }),
  ],
  entryPoints: ["./server/src/index.ts"],
  outfile: "./build/server.js",
  bundle: true,
  format: "esm",
};

if (import.meta.main) {
  if (Deno.args.includes("--watch")) {
    const context = await esbuild.context(options);
    await context.watch();
  } else {
    await esbuild.build(options);
    esbuild.stop();
  }
}
