
import type { PackageJson } from "@deno/dnt";
import { WalkFiles } from "./WalkDir.ts";

export const findAllPackage = (dirname: string) => {
  const packageJsons = new Map<string, PackageJson>();
  for (const entry of WalkFiles(dirname, { ignore: ["node_modules", "dist", "build", "scripts"] })) {
    if (entry.entryname === "package.json") {
      const packageJson = entry.readJson() as PackageJson;
      packageJsons.set(packageJson.name, packageJson);
    }
  }
  return packageJsons;
};
