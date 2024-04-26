// ex. scripts/build_npm.ts
import { build, emptyDir } from "@deno/dnt";
import fs from "node:fs";
import { fileURLToPath } from "node:url";
import { WalkFiles } from "./WalkDir.ts";

await emptyDir("./npm");

const res = [...WalkFiles(fileURLToPath(import.meta.resolve("../src")))].map((it) => "./src/" + it.relativepath);
await build({
  entryPoints: res,
  outDir: "./npm",
  shims: {
    // see JS docs for overview and more options
    deno: false,
  },
  mappings: {},
  package: Object.assign(fs.readFileSync("./package.json"), {
    version: Deno.args[0],
  }) as any,
  postBuild() {
    // steps to run after building and before running the tests
    // Deno.copyFileSync("LICENSE", "npm/LICENSE");
    Deno.copyFileSync("README.md", "npm/README.md");
  },
});
