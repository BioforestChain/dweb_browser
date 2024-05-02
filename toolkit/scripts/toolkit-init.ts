import fs from "node:fs";
import node_path from "node:path";
import picocolors from "npm:picocolors";
import { $ } from "../../scripts/helper/exec.ts";
import { findAllPackage } from "../../scripts/helper/findAllPackages.ts";
import { npmNameToFolder } from "../../scripts/helper/npmBuilder.ts";
import { rootResolve } from "../../scripts/helper/resolver.ts";

export const toolkitInit = async (options: { npmInstall?: boolean } = {}) => {
  const { npmInstall = true } = options;
  await npmInit();
  if (npmInstall) {
    await $(`pnpm install`);
  }
};
export const npmInit = async () => {
  const toolkitPackageJsons = findAllPackage(rootResolve("./toolkit"));

  const { default: importMap } = await import("./import_map.npm.json", { with: { type: "json" } });
  for (const npmName in importMap.imports) {
    const npmDir = npmNameToFolder(npmName);
    const packageJsonFile = node_path.resolve(npmDir, "./package.json");
    if (!fs.existsSync(packageJsonFile)) {
      fs.mkdirSync(npmDir, { recursive: true });
      // TODO 这里应该从项目中寻找同名的 package.json 源文件来进行拷贝
      fs.writeFileSync(packageJsonFile, JSON.stringify(toolkitPackageJsons.get(npmName) ?? { name: npmName }, null, 2));
      console.log(`npm package ${picocolors.green(npmName)} inited.`);
    }
  }
};
if (import.meta.main) {
  toolkitInit();
}
