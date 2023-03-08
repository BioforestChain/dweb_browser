import * as esbuild from "https://deno.land/x/esbuild@v0.17.11/mod.js";

esbuild
  .build({
    entryPoints: ["./demo/src/index.ts"],
    outfile: "./demo/src/index.js",
    format: "cjs",
    bundle: true,
  }).then(() => {
    esbuild.stop();
  });
