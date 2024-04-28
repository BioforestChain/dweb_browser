import { BuildOptions, PackageJson, build } from "@deno/dnt";
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
  const packageJson = options?.package ?? JSON.parse(fs.readFileSync(packageResolve("./package.json"), "utf-8"));
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

  //#region emptyDir(npmDir)
  // 这里要保留 package.json，因为在并发编译的时候，需要读取 package.json 以确保 workspace 能找到对应的项目所在的路径从而创造 symbol-link
  try {
    for (const item of Deno.readDirSync(npmDir)) {
      if (item.name && item.name !== "package.json") {
        Deno.removeSync(npmResolve(item.name), { recursive: true });
      }
    }
  } catch (err) {
    if (!(err instanceof Deno.errors.NotFound)) {
      throw err;
    }
    // if not exist. then create it
    Deno.mkdirSync(npmDir, { recursive: true });
  }
  //#endregion

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

const once = <R>(fun: () => Promise<R>) => {
  let res: Promise<R> | undefined;
  return () => (res ??= fun());
};

const regMap = new Map<string, () => Promise<void>>();
export const registryNpmBuilder = (config: Parameters<typeof npmBuilder>[0]) => {
  const { packageDir, options } = config;
  const packageResolve = (path: string) => fileURLToPath(new URL(path, packageDir));
  const packageJson: PackageJson =
    options?.package ?? JSON.parse(fs.readFileSync(packageResolve("./package.json"), "utf-8"));
  config.options = {
    ...config.options,
    package: packageJson,
  };
  regMap.set(
    packageJson.name,
    once(async () => {
      console.log(`🛫 START ${packageJson.name}`);
      /// 编译依赖，等待依赖编译完成
      for (const [key, version] of Object.entries(packageJson.dependencies || {})) {
        if (version.startsWith("workspace:")) {
          const depBuilder = regMap.get(key);
          if (!depBuilder) {
            console.warn(`❌ NO-FOUND DEPENDENCY ${key}\t---\t${packageJson.name}`);
            continue;
          }
          console.log(`⏳ WAITING DEPENDENCY ${key}\t---\t${packageJson.name}`);
          await depBuilder();
        }
      }
      // 编译自身
      console.log(`⏳ BUILDING ${packageJson.name}`);
      await npmBuilder(config);
      console.log(`✅ END ${packageJson.name}`);
    })
  );

  return () => regMap.get(packageJson.name)!();
};
