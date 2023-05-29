// import esbuild from 'npm:esbuild'
import { esbuild, esbuild_deno_loader, Flags } from "./deps.ts";

const esbuildFlags = Flags.parse(Deno.args, {
  boolean: ["bundle"],
  string: ["format", "target", "outfile"],
  default: {
    bundle: true,
    format: "esm",
    target: "es2020",
  },
});

const result = await esbuild.build({
  plugins: [
    ...esbuild_deno_loader.denoPlugins({
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

// esbuild.stop();
