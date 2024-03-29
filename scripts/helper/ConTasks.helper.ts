import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import type { $Task } from "./ConTasks.ts";
export const createBaseResolveTo = (baseDir: string = process.cwd()) => {
  if (baseDir.startsWith("file://")) {
    baseDir = fileURLToPath(baseDir);
  }
  const baseResolveTo = (...paths: string[]) => {
    return path.resolve(baseDir, ...paths);
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
export const esbuildTaskFactory = (config: {
  input: string | string[];
  outfile: string;
  importMap?: string;
  baseDir?: string;
}) => {
  const baseResolveTo = createBaseResolveTo(config.baseDir);
  const input =
    typeof config.input === "string"
      ? [baseResolveTo(config.input)]
      : config.input.map((input) => baseResolveTo(input));
  const output = baseResolveTo(config.outfile);
  const args = ["run", "-A", __esbuild, `--outfile`, output];
  input.forEach((input) => {
    args.push("--input", input);
  });
  if (config.importMap) {
    args.push(`--importMap`, baseResolveTo(config.importMap));
  }
  return {
    cmd: "deno",
    args,
    devAppendArgs: "--watch",
  } as $Task;
};
