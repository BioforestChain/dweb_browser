import { BuildOptions, build, emptyDir } from "@deno/dnt";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { WalkFiles } from "./WalkDir.ts";

const rootDir = import.meta.resolve("../../");
export const rootResolve = (path: string) => fileURLToPath(new URL(path, rootDir));
export const npmNameToFolderName = (name: string) => name.replace("/", "__");
export const npmNameToFolder = (name: string) => rootResolve(`./npm/${npmNameToFolderName(name)}`);
export const npmBuilder = async (config: {
  packageDir: string;
  version?: string;
  importMap?: string;
  options?: Partial<BuildOptions>;
  entryPointsDirName?: string | boolean;
}) => {
  const { packageDir, version, importMap, options, entryPointsDirName = "./src" } = config;
  const packageResolve = (path: string) => fileURLToPath(new URL(path, packageDir));
  const packageJson = JSON.parse(fs.readFileSync(packageResolve("./package.json"), "utf-8"));
  Object.assign(packageJson, {
    version: version ?? packageJson.version,
    // delete fields
    main: undefined,
    module: undefined,
    exports: undefined,
  });

  const customPostBuild = options?.postBuild;
  delete options?.postBuild;

  console.log(`\nstart dnt: ${packageJson.name}`);

  const npmDir = npmNameToFolder(packageJson.name);
  const npmResolve = (p: string) => path.resolve(npmDir, p);

  await emptyDir(npmDir);

  const srcEntryPoints =
    typeof entryPointsDirName === "string"
      ? [...WalkFiles(packageResolve(entryPointsDirName))]
          .filter((it) => it.entryname.endsWith(".ts"))
          .map((it) => it.relativepath)
      : [];

  await build({
    entryPoints: [
      { name: ".", path: packageResolve("./index.ts") },
      ...srcEntryPoints.map((name) => ({ name: `./${name}`, path: packageResolve(`${entryPointsDirName}/${name}`) })),
    ],
    outDir: npmDir,
    packageManager: "pnpm",
    shims: {
      // see JS docs for overview and more options
      deno: false,
    },
    test: false,
    importMap: importMap,
    package: packageJson,
    compilerOptions: {
      lib: ["DOM", "ES2020"],
      target: "ES2020",
      emitDecoratorMetadata: true,
      useDefineForClassFields: false,
    } as any,
    postBuild() {
      Deno.copyFileSync(rootResolve("./LICENSE"), npmResolve("./LICENSE"));
      // 拷贝必要的文件
      for (const filename of ["README.md", ".npmrc"]) {
        if (fs.existsSync(packageResolve(filename))) {
          Deno.copyFileSync(packageResolve(filename), npmResolve(filename));
        }
      }
      customPostBuild?.();
    },
    ...options,
  });
};
