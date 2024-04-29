import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import type { esbuild } from "../deps.ts";
import type { $Task } from "./ConTasks.ts";
export const createBaseResolveTo = (baseDir: string = process.cwd()) => {
  if (baseDir.startsWith("file://")) {
    baseDir = fileURLToPath(baseDir);
  }
  const baseResolveTo = (...paths: (string | URL)[]) => {
    let purePaths = paths.map((it) => it.toString());
    const filePathIndex = purePaths.findLastIndex((it) => it.startsWith("file:"));
    if (filePathIndex !== -1) {
      purePaths = purePaths.slice(filePathIndex);
      return path.resolve(fileURLToPath(purePaths[0]), ...purePaths.slice(1));
    }
    return path.resolve(baseDir, ...purePaths);
  };
  return baseResolveTo;
};

export const viteTaskFactory = (config: { inDir: string; outDir: string; viteConfig?: string; baseDir?: string }) => {
  const baseResolveTo = createBaseResolveTo(config.baseDir);
  const outDir = baseResolveTo(config.outDir);
  const inDir = baseResolveTo(config.inDir);
  const viteConfig = path.relative(
    inDir,
    config.viteConfig ? baseResolveTo(config.viteConfig) : baseResolveTo(config.inDir, "vite.config.ts")
  );
  return {
    cmd: "npx",
    args: ["vite", "build", `--outDir`, outDir, "--emptyOutDir", "-l=info", `-c`, viteConfig],
    logLineFilter: (log) => log.includes(config.outDir) === false && log.includes("Generated an empty chunk") === false,
    cwd: inDir,
    devAppendArgs: "--minify=false --watch",
    env: { FORCE_COLOR: "true" },
  } as $Task;
};

const __esbuild = fileURLToPath(import.meta.resolve("./ESBuild.ts"));
type ESbuildTaskOptionsBase = {
  input: string | string[];
  importMap?: string;
  baseDir?: string;
  tsconfig?: esbuild.TsconfigRaw;
};
export const esbuildTaskFactory = (
  config:
    | ({ outfile: string } & ESbuildTaskOptionsBase)
    //
    | ({ outdir: string } & ESbuildTaskOptionsBase)
) => {
  const baseResolveTo = createBaseResolveTo(config.baseDir);
  const args = ["run", "-A", __esbuild];
  const input =
    typeof config.input === "string"
      ? [baseResolveTo(config.input)]
      : config.input.map((input) => baseResolveTo(input));

  input.forEach((input) => {
    args.push("--input", input);
  });

  if ("outfile" in config) {
    const output = baseResolveTo(config.outfile);
    args.push(`--outfile`, output);
  } else {
    const output = baseResolveTo(config.outdir);
    args.push(`--outdir`, output);
  }

  if (config.importMap) {
    args.push(`--importMap`, baseResolveTo(config.importMap));
  }
  if (config.tsconfig) {
    args.push(`--tsconfig-raw`, JSON.stringify(config.tsconfig));
  }
  return {
    cmd: "deno",
    args,
    devAppendArgs: "--watch",
  } as $Task;
};
