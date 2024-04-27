import fs from "node:fs";
import path from "node:path";
import * as yaml from "yaml";
import { createBaseResolveTo } from "../helper/ConTasks.helper.ts";
import { $ } from "../helper/exec.ts";

export const toolkitInit = async () => {
  $.cwd(import.meta.resolve("../../"));

  await $(`git submodule update --init`);

  npmInit();
  await $(`pnpm install`);
};
const npmInit = () => {
  const rootResolve = createBaseResolveTo(import.meta.resolve("../../"));
  const packages: string[] = yaml.parse(fs.readFileSync(rootResolve("./pnpm-workspace.yaml"), "utf-8")).packages;

  for (const packageDirname of packages) {
    if (packageDirname.endsWith("/npm")) {
      const packageDir = rootResolve(packageDirname);
      const packageJsonFile = path.resolve(packageDir, "./package.json");
      if (!fs.existsSync(packageDir)) {
        fs.mkdirSync(packageDir, { recursive: true });
        fs.copyFileSync(path.resolve(packageDir, "../package.json"), packageJsonFile);
      }
    }
  }
  console.log("npm packages inited.");
};

if (import.meta.main) {
  toolkitInit();
}
