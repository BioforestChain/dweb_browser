import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";
import { resolveDenoJson } from "./scripts/helper/resolver.ts";

export default defineConfig(() => {
  const denoJson = resolveDenoJson();
  const localImports: Record<string, string> = {};

  /// 挑选出所有本地文件的映射配置
  for (const [key, value] of Object.entries(denoJson.imports)) {
    if (value.startsWith("./")) {
      localImports[key] = fileURLToPath(import.meta.resolve(value));
    }
  }
  /// 根据 alias 的特性，我们得先配置长的，再配置短的
  const orderedAlias: Record<string, string> = {};
  for (const key of Object.keys(localImports).sort().reverse()) {
    orderedAlias[key] = localImports[key];
  }

  return {
    esbuild: {
      tsconfigRaw: {
        compilerOptions: {
          experimentalDecorators: true,
        },
      },
    },
    test: {
      alias: orderedAlias,
    },
  };
});
