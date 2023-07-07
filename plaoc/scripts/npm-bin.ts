// ex. scripts/build_npm.ts
import * as semver from "https://deno.land/std@0.156.0/semver/mod.ts";
import { copyFileSync } from "node:fs";
import { dnt } from "../../scripts/deps.ts";

export const doBuidCore = async (config: {
  name: string;
  version: string;
  mainExports: string;
  buildFromRootDir: string;
  buildToRootDir: string;
  main?: string;
  importMap?: string;
  lib?: (dnt.LibName | string)[];
  devDependencies?: {
    [packageName: string]: string;
  };
}) => {
  const {
    version,
    buildFromRootDir,
    buildToRootDir,
    importMap,
    name,
    lib,
    main,
  } = config;
  console.log(`--- START BUILD: ${name} ${version} ---`);

  await dnt.emptyDir(buildToRootDir);

  const entryPoints: dnt.EntryPoint[] = [];
  // console.group("entry-point:", dirEntry.name, config);
  // 适配入口不是index的情况
  let entry = `${buildFromRootDir}${main ? main : "/index.ts"}`;
  if (buildFromRootDir.includes(".ts")) {
    entry = buildFromRootDir;
  }
  entryPoints.push({
    kind: "bin",
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
    /**
     * @TODO should ignore errors:
     * 1. TS2691
     */
    typeCheck: false,
    test: false,
    shims: {
      deno: true,
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
      fromFile = `${buildFromRootDir.slice(
        0,
        buildFromRootDir.lastIndexOf("/")
      )}/${base}`;
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
      if (
        !(
          release === "major" ||
          release === "minor" ||
          release === "patch" ||
          (release === "prerelease" && typeof identifier === "string")
        )
      ) {
        console.error(
          "请输入正确的 ReleaseType: major, minor, patch, prerelease:identifier"
        );
        Deno.exit(0);
      }
      // major, minor, patch, or prerelease
      getVersion = (version) =>
        semver.inc(version, release, undefined, identifier) || version;
    } else {
      const semver_version = semver.minVersion(version_input);
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
  const getVersion = getVersionGenerator(args[0]);
  try {
    const npmConfigs = (await import(file, { assert: { type: "json" } }))
      .default;

    for (const config of npmConfigs) {
      await doBuidCore({
        ...config,
        version: getVersion(config.version),
      });
    }
  } catch (error) {
    throw new Error(error);
  }
};

export const doBuild = async (args = Deno.args) => {
  const targets = args.filter((a) => /^\w/.test(a));
  const rest = args.filter((a) => /^\w/.test(a) === false);

  const target = targets[0] ?? "cli";

  await doBuildFromJson(import.meta.resolve(`./npm.${target}.json`), rest);
};

if (import.meta.main) {
  doBuild();
}
