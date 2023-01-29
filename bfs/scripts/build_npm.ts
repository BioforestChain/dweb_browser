import { copySync } from "https://deno.land/std@0.156.0/fs/mod.ts";
import {
  build,
  emptyDir,
  EntryPoint,
  LibName,
} from "https://deno.land/x/dnt@0.31.0/mod.ts";

export const doBuid = async (config: {
  name: string;
  version: string;
  mainExports: string;
  buildFromRootDir: string;
  buildToRootDir: string;
  importMap?: string;
  lib?: (LibName | string)[];
  devDependencies?: {
    [packageName: string]: string;
  };
}) => {
  const { version, buildFromRootDir, buildToRootDir, importMap, name, lib } =
    config;
  console.log(`--- START BUILD: ${name} ${version} ---`);

  await emptyDir(buildToRootDir);

  const entryPoints: EntryPoint[] = [];
  // console.group("entry-point:", dirEntry.name, config);
  // 适配入口不是index的情况
  let entry = `${buildFromRootDir}/index.ts`;
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
    const getOrder = (ep: EntryPoint) => orderMap.get(ep.name) || 1;
    entryPoints.sort((a, b) => getOrder(b) - getOrder(a));
  }
  // console.group("entryPoints ===========> ", JSON.stringify(entryPoints));

  await build({
    importMap: importMap,
    entryPoints: entryPoints,
    outDir: buildToRootDir,
    scriptModule: false,
    /**
     * @TODO should ignore errors:
     * 1. TS2691
     */
    typeCheck: true,
    test: false,
    shims: {
      deno: "dev", custom: [
        // {
        //   module: "https://cdn.esm.sh/jsdom@20.0.3",
        //   globalNames: ["HTMLElement"],
        // }
      ]
    },
    compilerOptions: {
      target: "ES2020",
      importHelpers: true,
      emitDecoratorMetadata: true,
      lib: lib as LibName[],
    },
    packageManager: "npm",
    package: {
      // package.json properties
      name: name,
      version: version,
      description: `plaoc system runtime`,
      license: "MIT",
      repository: {
        type: "git",
        url: "git+https://github.com/BioforestChain/plaoc.git",
      },
      bugs: {
        url: "https://github.com/BioforestChain/plaoc/issues",
      },
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
      copySync(fromFilename, toFilename, { overwrite: true });
    } catch (err) {
      if (err instanceof Deno.errors.NotFound === false) {
        throw err;
      }
    }
  }
};

import * as semver from "https://deno.land/std@0.156.0/semver/mod.ts";

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

export const doBuildFromJson = async (file: string, args = Deno.args) => {
  const getVersion = getVersionGenerator(args[0]);
  try {
    const npmConfigs = (await import(file, { assert: { type: "json" } }))
      .default;

    for (const config of npmConfigs) {
      await doBuid({
        ...config,
        version: getVersion(config.version),
      });
    }
  } catch (error) {
    throw new Error(error);
  }
};

if (import.meta.main) {
  let target = "gui";
  if (Deno.args[1]) {
    target = Deno.args[1];
  }
  // deno-lint-ignore no-explicit-any
  await doBuildFromJson((import.meta as any).resolve(`./npm.${target}.json`));
}
