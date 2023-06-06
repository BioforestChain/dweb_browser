import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { dnt } from "../../scripts/deps.ts";
import { WalkFiles } from "./WalkDir.ts";
// await emptyDir("./npm");

/// before build
Deno.copyFileSync(".npmrc", "electron/.npmrc");

await dnt.build({
  entryPoints: ["./src/index.ts"],
  outDir: "./electron",
  shims: {
    // see JS docs for overview and more options
    deno: true,
  },
  importMap: "import_map.json",
  typeCheck: false,
  test: false,
  esModule: false,
  compilerOptions: {
    inlineSources: true,
    sourceMap: true,
  },
  package: {
    // package.json properties
    name: "@dweb-browser/desktop-sdk",
    version: Deno.args.filter((arg) => /^\d/.test(arg))[0],
    description: "Dweb Browser Development Kit",
    license: "MIT",
    config: {
      electron_mirror: "https://npm.taobao.org/mirrors/electron/",
    },
    scripts: {
      start: "electron ./",
    },
  },
  packageManager: "yarn",
  postBuild() {
    type $SourceMap = {
      version: number;
      file: string;
      sourceRoot: string;
      sources: string[];
      names: string[];
      mappings: string;
      sourcesContent: string[];
    };
    const originSrcPath = fileURLToPath(import.meta.resolve("../"));
    for (const entry of WalkFiles("./electron/script")) {
      if (entry.entryname.endsWith(".js.map")) {
        const sourceMap = entry.readJson<$SourceMap>();
        sourceMap.sources = sourceMap.sources.map((source, index) => {
          const denoFilepath = path.resolve(
            originSrcPath,
            entry.relativepath,
            source
          );
          if (fs.existsSync(denoFilepath)) {
            sourceMap.sourcesContent[index] =
              Deno.readTextFileSync(denoFilepath);
          }

          return path.relative(entry.dirpath, denoFilepath);
        });
        entry.writeJson(sourceMap);
      }
    }

    /// 启动
    new Deno.Command("pnpm", {
      args: ["start", ...Deno.args.slice(Deno.args.indexOf("--start") + 1)],
      cwd: "./electron",
    }).spawn();
    // steps to run after building and before running the tests
    // Deno.copyFileSync("README.md", "npm/README.md");
  },
});
