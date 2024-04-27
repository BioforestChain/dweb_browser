// ex. scripts/build_npm.ts
import * as semver from "https://deno.land/std@0.220.1/semver/mod.ts";
import { copyFileSync, watch } from "node:fs";
import path from "node:path";
import * as dnt from "@deno/dnt";

export const doBuidCore = async (config: {
  name: string;
  version: string;
  mainExports: string;
  buildFromRootDir: string;
  entryFile?: string;
  buildToRootDir: string;
  importMap?: string;
  denoShim?: boolean;
  lib?: (dnt.LibName | string)[];
  devDependencies?: {
    [packageName: string]: string;
  };
  watchDir?: string;
  watch?: boolean;
  dev?: boolean;
}) => {
  const { version, buildFromRootDir, entryFile = "index.ts", buildToRootDir, importMap, name, lib } = config;
  console.log(`--- START BUILD: ${name} ${version} ---`);

  await dnt.emptyDir(buildToRootDir);

  const entryPoints: dnt.EntryPoint[] = [];
  // console.group("entry-point:", dirEntry.name, config);
  // 适配入口不是index的情况
  let entry = path.resolve(buildFromRootDir, entryFile);
  if (buildFromRootDir.includes(".ts")) {
    entry = buildFromRootDir;
  }
  entryPoints.push({
    name: config.mainExports,
    path: entry,
  });
  console.group("buildFromDir :", buildFromRootDir);
  // console.groupEnd();

  const orderMap = new Map([[".", 100]]);
  {
    const getOrder = (ep: dnt.EntryPoint) => orderMap.get(ep.name) || 1;
    entryPoints.sort((a, b) => getOrder(b) - getOrder(a));
  }

  await dnt.build({
    importMap: importMap,
    entryPoints: entryPoints,
    outDir: buildToRootDir,
    declaration: "separate",
    scriptModule: false,
    typeCheck: "both",
    test: false,
    shims: {
      deno: config.denoShim,
      custom: [],
    },
    compilerOptions: {
      target: "ES2020",
      importHelpers: true,
      emitDecoratorMetadata: true,
      lib: lib as dnt.LibName[],
    },
    packageManager: "npm",
    package: {
      // package.json properties
      name: name,
      version: version,
      description: `WebApp-Framework In DwebBrowser`,
      license: "MIT",
      repository: {
        type: "git",
        url: "git+https://github.com/BioforestChain/dweb_browser.git",
      },
      bugs: {
        url: "https://github.com/BioforestChain/dweb_browser/issues",
      },
      keywords: ["plaoc", "dweb"],
      devDependencies: config.devDependencies ?? {},
    },
  });

  // post build steps
  for (const base of ["README.md", "LICENSE"]) {
    // 适配入口不是index的情况
    let fromFile = `${buildFromRootDir}/${base}`;
    if (buildFromRootDir.includes(".ts")) {
      fromFile = `${buildFromRootDir.slice(0, buildFromRootDir.lastIndexOf("/"))}/${base}`;
    }
    const fromFilename = fromFile;
    const toFilename = `${buildToRootDir}/${base}`;
    try {
      copyFileSync(fromFilename, toFilename);
    } catch (err) {
      if (err instanceof Deno.errors.NotFound === false) {
        throw err;
      }
    }
  }

  if (config.watch && config.watchDir) {
    const watcher = watch(path.resolve(Deno.cwd(), config.watchDir), { recursive: true });
    watcher.addListener("change", () => {
      watcher.close();
      doBuidCore(config);
    });
  }
};

export const getVersionGenerator = (version_input?: string) => {
  let getVersion = (version: string) => {
    return version;
  };
  if (version_input) {
    if (version_input.startsWith("+")) {
      const [release, identifier] = version_input
        .slice(1)
        .split(/\:/)
        .map((v, index) => {
          if (index === 0) {
            switch (v) {
              case "1":
                return "patch";
              case "1.0":
                return "minor";
              case "1.0.0":
                return "major";
              case "pre":
                return "prerelease";
            }
          }
          return v;
        });
      if (!(release === "major" || release === "minor" || release === "patch" || release === "prerelease")) {
        console.error("请输入正确的 ReleaseType: major, minor, patch, prerelease:identifier");
        Deno.exit(0);
      }
      // major, minor, patch, or prerelease
      // @ts-ignore
      getVersion = (version) => semver.inc(version, release, undefined, identifier) || version;
    } else {
      const semver_version = (() => {
        try {
          return semver.minVersion(version_input);
        } catch {
          return null;
        }
      })();
      if (semver_version === null) {
        console.error("请输入正确的待发布版本号");
        Deno.exit(0);
      }

      getVersion = () => semver_version.toString();
    }
  }
  return getVersion;
};

export const doBuildFromJson = async (file: string, args: string[]) => {
  const getVersion = getVersionGenerator(args.find((a) => !a.startsWith("-")));
  try {
    const npmConfigs = (await import(file, { assert: { type: "json" } })).default;

    for (const config of npmConfigs) {
      await doBuidCore({
        ...config,
        version: getVersion(config.version),
        watch: args.includes("--watch"),
        dev: args.includes("--dev"),
      });
    }
  } catch (error) {
    throw new Error(error);
  }
};

export const doBuild = async (args = Deno.args) => {
  /**
   * 这里导入源码，目的是为了让 deno 的 watch 可以正确工作 为此，我们需要做一些基础的垫片工作
   */
  {
    if (typeof HTMLElement === "undefined") {
      const noop = () => {};
      Object.assign(globalThis, {
        HTMLElement: class HTMLElement {},
        customElements: { define: noop },
        document: { addEventListener: noop },
      });
    }
  }
  const targets = args.filter((a) => /^\w/.test(a));
  const rest = args.filter((a) => /^\w/.test(a) === false);

  const target = targets[0] ?? "client";

  await doBuildFromJson(import.meta.resolve(`./npm.${target}.json`), rest);
};

if (import.meta.main) {
  doBuild();
}
