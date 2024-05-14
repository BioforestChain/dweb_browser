import { createFilter } from "@rollup/pluginutils";
import { transform } from "@swc/core";
import type { Plugin } from "vite";
import { defineConfig } from "vitest/config";

export default defineConfig({
  esbuild: false,
  plugins: [RollupPluginSwc()],
  test: {
    alias: {
      "@dweb-browser/helper/": new URL("./toolkit/dweb-helper/src/", import.meta.url).pathname,
    },
  },
});

//#region rollup-plugin-swc
export const queryRE = /\?.*$/;
export const hashRE = /#.*$/;
export const cleanUrl = (url: string) => url.replace(hashRE, "").replace(queryRE, "");

export function RollupPluginSwc(): Plugin {
  const filter = createFilter(/\.(tsx?|jsx)$/, /\.js$/);

  return {
    name: "rollup-plugin-swc",
    async transform(code, id) {
      if (filter(id) || filter(cleanUrl(id))) {
        const result = await transform(code, {
          jsc: {
            target: "es2022",
            parser: {
              syntax: "typescript",
              decorators: true,
            },
            transform: {
              legacyDecorator: true,
              decoratorMetadata: true,
            },
          },
          filename: id,
        });
        return {
          code: result.code,
          map: result.map,
        };
      }
    },
  };
}
