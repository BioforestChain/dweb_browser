import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { dnt } from "../../scripts/deps.ts";
import { doPubCore } from "../../scripts/helper/NpmPub.ts";
import { WalkFiles } from "./WalkDir.ts";
import { assetsTasks } from "./assets-tasks.ts";
const doBundle = async () => {
  await assetsTasks.spawn(["js-process.worker.js"]).afterComplete();
};
// await emptyDir("./npm");
const workspaceDir = fileURLToPath(import.meta.resolve("../"));
const resolveTo = (to: string) => path.resolve(workspaceDir, to);
const readPackageJson = () => {
  const packageJsonPath = resolveTo("./build/js-process/package.json");
  if (fs.existsSync(packageJsonPath)) {
    const ROOT_PACKAGE = JSON.parse(fs.readFileSync(packageJsonPath, "utf-8")) as { version: string };
    return ROOT_PACKAGE;
  }
};

export const doBuild = async (args = Deno.args, _version?: string) => {
  const isPub = args.includes("--pub");
  const version = _version || args.filter((arg) => /^\d/.test(arg))[0] || readPackageJson()?.version || "0.0.0";

  /// before build
  Deno.copyFileSync(resolveTo(".npmrc"), resolveTo("electron/.npmrc"));
  /// do build
  await dnt.build({
    entryPoints: [resolveTo("./src/browser/js-process/assets/module.ts")],
    outDir: resolveTo("./build/js-process"),
    shims: {
      // see JS docs for overview and more options
      deno: false,
    },
    mappings: {},
    // importMap: resolveTo("./src/browser/js-process/assets/worker/import_map.json"),
    typeCheck: false,
    test: false,
    esModule: true,
    compilerOptions: {
      inlineSources: true,
      sourceMap: true,
      lib: ["WebWorker", "ES2021"],
    },
    skipSourceOutput: false,
    /// package.json properties
    package: {
      name: "@dweb-browser/js-process",
      companyName: "Bnqkl, Inc.",
      copyright: "Copyright © 2023 Bnqkl, Inc.",
      homepage: "https://github.com/BioforestChain/dweb_browser",
      version,
      description: "the js-process runtime in dweb-browser",
      license: "MIT",
      author: "Bnqkl Dweb Team",
      types: "./src/module.ts",
      exports: {
        ".": {
          types: "./src/module.ts",
          import: "./esm/module.js",
          require: "./script/module.js",
        },
      },
      dependencies: {
        "@dweb-browser/desktop": version,
      },
    },
    packageManager: "yarn",
    async postBuild() {
      /// STEP1:
      Deno.copyFileSync(resolveTo("logo.png"), resolveTo("electron/logo.png"));

      /// STEP2: 强行进行源码映射
      type $SourceMap = {
        version: number;
        file: string;
        sourceRoot: string;
        sources: string[];
        names: string[];
        mappings: string;
        sourcesContent: string[];
      };

      for (const entry of WalkFiles(resolveTo("./electron/script"))) {
        if (entry.entryname.endsWith(".js.map")) {
          const sourceMap = entry.readJson<$SourceMap>();
          sourceMap.sources = sourceMap.sources.map((source, index) => {
            const denoFilepath = path.resolve(workspaceDir, entry.relativepath, source);
            if (fs.existsSync(denoFilepath)) {
              sourceMap.sourcesContent[index] = Deno.readTextFileSync(denoFilepath);
            }

            return path.relative(entry.dirpath, denoFilepath);
          });
          entry.writeJson(sourceMap);
        }
      }

      await doBundle();
      if (isPub) {
        await doPubCore(resolveTo("./build/js-process"));
      }
    },
  });
};

if (import.meta.main) {
  doBuild();
}
