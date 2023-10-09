import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { dnt } from "../../scripts/deps.ts";
import { whichSync } from "../../scripts/helper/WhichCommand.ts";
import { WalkFiles } from "./WalkDir.ts";
import { doBundle } from "./bundle.ts";
// await emptyDir("./npm");
const workspaceDir = fileURLToPath(import.meta.resolve("../"));
const resolveTo = (to: string) => path.resolve(workspaceDir, to);
const readPackageJson = () => {
  const packageJsonPath = resolveTo("./electron/package.json");
  if (fs.existsSync(packageJsonPath)) {
    const ROOT_PACKAGE = JSON.parse(fs.readFileSync(packageJsonPath, "utf-8")) as { version: string };
    return ROOT_PACKAGE;
  }
};

/// before build
Deno.copyFileSync(resolveTo(".npmrc"), resolveTo("electron/.npmrc"));
const ID = "com.instinct.macApp1st";
const productName = "Dweb Browser";
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
  /// package.json properties
  package: {
    appBundleId: ID,
    name: "@dweb-browser/desktop-sdk",
    companyName: "Bnqkl, Inc.",
    appCopyright: "Copyright © 2023 Bnqkl, Inc.",
    productName: productName,
    homepage: "https://github.com/BioforestChain/dweb_browser",
    version: Deno.args.filter((arg) => /^\d/.test(arg))[0] || readPackageJson()?.version || "0.0.0",
    description: "Distributed web browser",
    license: "MIT",
    config: {
      electron_mirror: "https://npm.taobao.org/mirrors/electron/",
    },
    scripts: {
      start: "electron ./",
    },
    author: "Bnqkl Dweb Team",
    main: "./bundle/index.js",
    bin: {
      "dweb-browser-devtools": "./bundle/index.js",
    },
    darwinDarkModeSupport: true,
    protocols: [
      {
        name: productName,
        schemes: ["dweb"],
      },
    ],
    build: {
      appId: ID,
      productName: productName,
      artifactName: "${productName}-${version}-${arch}.${ext}",
      asar: true,
      // icon: "/logo.svg",
      files: ["assets", "bundle", "!node_modules"],
      directories: { output: "../build" },
      extraResources: [
        {
          from: "./icons",
          to: "./icons",
        },
      ],
      mac: {
        icon: "./icons/mac/icon.icns",
        category: "public.app-category.developer-tools",
        target: {
          target: "default",
          arch: ["x64", "arm64"],
        },
        provisioningProfile: "scripts/macApp1st_prov.provisionprofile",
      },
      win: {
        icon: "./icons/win/icon.ico",
        target: {
          target: "portable",
          arch: ["x64"], //, "arm64"
        },
        publisherName: "Bnqkl Dweb Team",
      },
      linux: {
        icon: "./icons/mac/icon.icns",
        category: "Development;Network",
        maintainer: "Bnqkl Dweb Team",
        mimeTypes: ["x-scheme-handler/dweb"],
        desktop: {
          StartupNotify: "false",
          StartupWMClass: "dweb",
          Encoding: "UTF-8",
          MimeType: "x-scheme-handler/dweb",
          // exec: "dweb %U"
        },
        target: [
          { target: "deb", arch: ["x64", "arm64"] },
          // { target: "rpm", arch: ["x64", "arm64"] },
          // { target: "tar.xz", arch: ["x64", "arm64"] },
        ],
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
          const denoFilepath = path.resolve(workspaceDir, entry.relativepath, source);
          if (fs.existsSync(denoFilepath)) {
            sourceMap.sourcesContent[index] = Deno.readTextFileSync(denoFilepath);
          }

          return path.relative(entry.dirpath, denoFilepath);
        });
        entry.writeJson(sourceMap);
      }
    }

    /// STEP3: fix for electron-builder
    const updatePackageJson = (updater: (packageJson: any) => unknown) => {
      const packageJson = readPackageJson();
      fs.writeFileSync(
        resolveTo("./electron/package.json"),
        JSON.stringify(updater(packageJson) || packageJson, null, 2)
      );
    };
    updatePackageJson((packageJson) => {
      const moveDepToDev = (name: string) => {
        if (!packageJson.dependencies) return;
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
      const cmd = whichSync("pnpm");
      new Deno.Command(cmd!, {
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
