import fs from "node:fs";
import path from "node:path";
import { $ } from "../../scripts/helper/exec.ts";
import { npmNameToFolder } from "../../scripts/helper/npmBuilder.ts";

export const toolkitInit = async () => {
  await npmInit();
  await $(`pnpm install`);
};
export const npmInit = async () => {
  const { default: importMap } = await import("./import_map.npm.json", { with: { type: "json" } });
  for (const npmName in importMap.imports) {
    const npmDir = npmNameToFolder(npmName);
    const packageJsonFile = path.resolve(npmDir, "./package.json");
    if (!fs.existsSync(packageJsonFile)) {
      fs.mkdirSync(npmDir, { recursive: true });
      // TODO 这里应该从项目中寻找同名的 package.json 源文件来进行拷贝
      fs.writeFileSync(packageJsonFile, JSON.stringify({ name: npmName }));
    }
  }

  console.log("npm packages inited.");
};

if (import.meta.main) {
  toolkitInit();
}
