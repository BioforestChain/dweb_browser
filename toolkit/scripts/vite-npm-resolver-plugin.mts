import { createProjectResolver, readJson, walkDirs } from "@gaubee/nodekit";
import fs from "node:fs";
import path from "node:path";
import { type PluginOption } from "vite";

export const toolkitResolverPlugin = (): PluginOption => {
  const rootResolver = createProjectResolver(undefined, "pnpm-workspace.yaml");
  const npmDir = rootResolver("npm");
  console.log("QAQ npmDir", npmDir);
  const resolveMap: Record<string, string> = {};
  const getImport = (dirname: string, importValue: string | { import: string } | { default: string }) => {
    if (typeof importValue === "string") {
      return path.join(dirname, importValue);
    } else if ("import" in importValue) {
      return path.join(dirname, importValue.import);
    } else if ("default" in importValue) {
      return path.join(dirname, importValue.default);
    }
  };
  for (const dirEntry of walkDirs(npmDir, { deepth: 1 })) {
    const packageJsonFilepath = path.join(dirEntry.path, "package.json");
    if (fs.existsSync(packageJsonFilepath)) {
      const packageJson = readJson(packageJsonFilepath);
      for (const key in packageJson.exports) {
        let resolveId: string | undefined;
        if (key === ".") {
          resolveId = packageJson.name;
        } else if (key.startsWith("./")) {
          resolveId = packageJson.name + key.slice(1);
        }

        if (resolveId) {
          const resolvePath = getImport(dirEntry.path, packageJson.exports[key]);
          if (resolvePath) {
            resolveMap[resolveId] = resolvePath;
          }
        }
      }
    }
  }
  return {
    name: "toolkit-resolver",
    enforce: "pre",
    resolveId(source, _importer) {
      return resolveMap[source];
    },
  };
};
