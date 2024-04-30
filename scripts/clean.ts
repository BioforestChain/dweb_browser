import fs from "node:fs";
import { IgnoreGlob } from "./helper/IgnoreGlob.ts";
import { WalkAny, WalkDirs, WalkOptions } from "./helper/WalkDir.ts";
import { rootResolve } from "./helper/npmBuilder.ts";

const gitignore = IgnoreGlob.fromIgnoreFile(rootResolve("./.gitignore"));
const cleanFiles = (dirname: string, options?: WalkOptions) => {
  //   console.log(dirname)
  for (const entry of WalkAny(dirname, { workspace: rootResolve(), ...options })) {
    if (gitignore.isIgnore(entry.workspacepath)) {
      console.log("rm", entry.workspacepath);
      if(fs.existsSync(entry.workspacepath)) {
        fs.rmSync(entry.entrypath, { recursive: true });
      }
    }
  }
};
const doRmNpm = () => {
  cleanFiles(rootResolve("./npm"), { self: true });
};
const doRmAssets = () => {
  cleanFiles(rootResolve("./assets"), { deepth: 1 });
};
const doRmKmpResources = () => {
  for (const moduleEntry of WalkDirs(rootResolve("./next/kmp"), { deepth: 3, ignore: [".*", "build"] })) {
    if (moduleEntry.relativepath.endsWith("Main")) {
      cleanFiles(moduleEntry.entrypath + "/composeResources", { self: true });
      cleanFiles(moduleEntry.entrypath + "/resources", { self: true });
    }
  }
};

export const doClear = (args = Deno.args) => {
  const taskMap = new Map([
    ["npm", doRmNpm],
    ["assets", doRmAssets],
    ["kmpRes", doRmKmpResources],
  ]);

  const nameFilter = args.filter((arg) => !arg.startsWith("-"));

  for (const [name, action] of taskMap) {
    if (nameFilter.length === 0 || nameFilter.some((it) => name.startsWith(it))) {
      action();
    }
  }
};

if (import.meta.main) {
  doClear();
}
