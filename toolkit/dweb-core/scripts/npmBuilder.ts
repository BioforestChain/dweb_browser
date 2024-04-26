import { BuildOptions, build, emptyDir } from "@deno/dnt";
import fs from "node:fs";
import { fileURLToPath } from "node:url";
import { WalkFiles } from "./WalkDir.ts";

export const npmBuilder = async (config: {
  rootUrl: string;
  version?: string;
  importMap?: string;
  options?: Partial<BuildOptions>;
  entryPointsDirName?: string | boolean;
}) => {
  const { rootUrl, version, importMap, options, entryPointsDirName = "./src" } = config;
  const rootResolve = (path: string) => fileURLToPath(new URL(path, rootUrl));
  const npmDir = rootResolve("./npm");
  await emptyDir(npmDir);

  const srcEntryPoints =
    typeof entryPointsDirName === "string"
      ? [...WalkFiles(rootResolve(entryPointsDirName))].map((it) => it.relativepath)
      : [];

  const packageJson = JSON.parse(fs.readFileSync(rootResolve("./package.json"), "utf-8"));
  if (version) {
    Object.assign(packageJson, { version: version });
  }
  // for (const [depName, depVersion] of Object.entries(packageJson.dependencies as Record<string, string>)) {
  //   if (depVersion.startsWith("workspace:")) {
  //     delete packageJson.dependencies[depName];
  //   }
  // }

  await build({
    entryPoints: [
      { name: ".", path: rootResolve("./index.ts") },
      ...srcEntryPoints.map((name) => ({ name: `./${name}`, path: `${entryPointsDirName}/${name}` })),
    ],
    outDir: npmDir,
    packageManager: "pnpm",
    shims: {
      // see JS docs for overview and more options
      deno: false,
    },
    test: false,
    mappings: {
      // "https://esm.sh/@dweb-browser/helper":{
      // }
    },
    importMap: importMap, //import.meta.resolve("../../../deno.jsonc"),
    package: packageJson,
    compilerOptions: {
      lib: ["DOM", "ES2022"],
      useDefineForClassFields: false,
      experimentalDecorators: true,
      emitDecoratorMetadata: true,
      noEmit: false,
      allowImportingTsExtensions: true,
    } as any,
    postBuild() {
      // steps to run after building and before running the tests
      // Deno.copyFileSync("LICENSE", "npm/LICENSE");
      // Deno.copyFileSync("README.md", "npm/README.md");
    },
    ...options,
  });
};
