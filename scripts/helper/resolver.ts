import fs from "node:fs";
import { createBaseResolveTo } from "./resolveTo.ts";

/// 为了兼容 vitest，这里没有用 import.meta.resolve
export const rootDir = new URL("../../", import.meta.url).href;
export const rootResolve = createBaseResolveTo(rootDir);

let denoConfig:
  | {
      imports: Record<string, string>;
    }
  | undefined;
export const resolveDenoJson = () => {
  return (denoConfig ??= Function(`return(${fs.readFileSync(rootResolve("./deno.jsonc"))})`)()) as NonNullable<
    typeof denoConfig
  >;
};
