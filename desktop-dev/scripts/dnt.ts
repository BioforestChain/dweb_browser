import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { dnt } from "../../scripts/deps.ts";
import { WalkFiles } from "./WalkDir.ts";
import { doBundle } from "./bundle.ts";
// await emptyDir("./npm");
const workspaceDir = fileURLToPath(import.meta.resolve("../"));
const resolveTo = (to: string) => path.resolve(workspaceDir, to);

/// before build
Deno.copyFileSync(resolveTo(".npmrc"), resolveTo("electron/.npmrc"));

/// do build
await dnt.build({
  entryPoints: [resolveTo("./src/index.dev.ts")],
  outDir: resolveTo("./electron"),
  shims: {
    // see JS docs for overview and more options
    deno: true,
  },
  importMap: resolveTo("import_map.json"),
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
    version: Deno.args.filter((arg) => /^\d/.test(arg))[0] || "0.0.0",
    description: "Dweb Browser Development Kit",
    license: "MIT",
    config: {
      electron_mirror: "https://npm.taobao.org/mirrors/electron/",
    },
    scripts: {
      start: "electron ./script/index.dev.js",
    },
    author: "Bnqkl Dweb Team",
    main: "./bundle/index.js",
    bin: {
      "dweb-browser-devtools": "./bundle/index.js",
    },
    build: {
      appId: "devtools.dweb-browser.org",
      productName: "dweb-browser-devtools", // 这个一定要写，不然 name 使用 @ 开头，会带来打包异常
      asar: true,
      icon: "./logo.png",
      files: ["assets", "bundle", "!node_modules"],
      directories: { output: "../build" },
      mac: {
        category: "public.app-category.developer-tools",
        target: "dmg",
      },
      win: {
        target: "portable",
        publisherName: "Bnqkl Dweb Team",
      },
    },
  },
  packageManager: "yarn",
  postBuild() {
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
          const denoFilepath = path.resolve(
            workspaceDir,
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

    /// STEP3: fix for electron-builder
    const updatePackageJson = (updater: (packageJson: any) => unknown) => {
      const packageJson = JSON.parse(
        fs.readFileSync(resolveTo("./electron/package.json"), "utf-8")
      );
      fs.writeFileSync(
        resolveTo("./electron/package.json"),
        JSON.stringify(updater(packageJson) || packageJson, null, 2)
      );
    };
    updatePackageJson((packageJson) => {
      const moveDepToDev = (name: string) => {
        packageJson.devDependencies[name] = packageJson.dependencies[name];
        delete packageJson.dependencies[name];
      };
      moveDepToDev("electron");
      moveDepToDev("lit");
    });

    /// STEP4.1: try start app in dev-mode
    if (Deno.args.includes("--start")) {
      /// use dev enterpoint
      updatePackageJson((packageJson) => {
        packageJson.main = "./script/index.dev.js";
      });
      /// 启动
      new Deno.Command("pnpm", {
        args: ["start", ...Deno.args.slice(Deno.args.indexOf("--start") + 1)],
        cwd: resolveTo("./electron"),
      }).spawn();
    }
    /// STEP4.2: bundle app for prod-mode
    else {
      doBundle();
    }
  },
});
