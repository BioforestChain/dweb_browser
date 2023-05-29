// import esbuild from 'npm:esbuild'
import { parse } from "https://deno.land/std@0.184.0/flags/mod.ts";
import * as esbuild from "https://deno.land/x/esbuild@v0.17.19/mod.js";
import { denoPlugins } from "https://deno.land/x/esbuild_deno_loader@0.7.0/mod.ts";

const esbuildFlags = parse(Deno.args, {
  boolean: ["bundle"],
  string: ["format", "target", "outfile"],
  default: {
    bundle: true,
    format: "esm",
    target: "es2020",
  },
});

console.log("esbuildFlags", esbuildFlags);

const result = await esbuild.build({
  plugins: [
    ...denoPlugins({
      // importMapURL: import.meta.resolve("../import_map.json") ,
      configPath: import.meta.resolve("../deno.json"),
    }),
  ],
  entryPoints: esbuildFlags._.filter((s) => typeof s === "string") as string[],
  outfile: esbuildFlags.outfile,
  bundle: esbuildFlags.bundle,
  format: esbuildFlags.format as esbuild.Format,
  target: esbuildFlags.target,
});
console.log(result.outputFiles);

// esbuild.stop();
