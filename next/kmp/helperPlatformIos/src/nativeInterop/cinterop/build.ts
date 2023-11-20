import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

import { WalkFiles } from "../../../../../../plaoc/cli/helper/walk-dir.ts";
import { which } from "../../../../../../scripts/helper/WhichCommand.ts";
import { doArchiveTask } from "./archive.ts";
import { doCreateXcTask } from "./create-xc.ts";
import { __dirname, runTasks } from "./util.ts";

export const doBuildTask = async () => {
  const xcodebuild = await which("xcodebuild");

  if(!xcodebuild) {
    return;
  }

  //#region 用于判断xcode是否安装，存在安装了xcode commandLineTools确没安装xcode的情况
  const pkgutil = await which("pkgutil");

  if(!pkgutil) {
    return;
  }

  const task = new Deno.Command(pkgutil, {
    args: ["--pkg-info=com.apple.pkg.CLTools_Executables"],
    stdin: "inherit",
    stdout: "inherit",
  });
  
  const code = (await task.output()).code;

  if(code !== 0) {
    return;
  }
  //#endregion

  const writeFileHash = calcHash();
  if (!writeFileHash) {
    console.log("build cached!!");
    return 0;
  }
  return await runTasks(doArchiveTask, doCreateXcTask, async () => {
    writeFileHash();
    console.log("build success!!");
    return 0;
  });
};
const calcHash = (): (() => void) | undefined => {
  const hashBuilder = crypto.createHash("sha256");
  for (const entry of WalkFiles(path.resolve(__dirname, "./DwebPlatformIosKit"))) {
    hashBuilder.update(entry.entrypath);
    const stat = fs.statSync(entry.entrypath);
    hashBuilder.update(JSON.stringify([stat.size, stat.ctimeMs, stat.mtimeMs]));
  }
  const fileHash = hashBuilder.digest("hex");

  const buildCacheHash = path.resolve(__dirname, "./archives/build-cache.temp");
  const nochange = fs.existsSync(buildCacheHash) && fs.readFileSync(buildCacheHash, "utf-8") === fileHash;
  if (!nochange) {
    return () => {
      fs.writeFileSync(buildCacheHash, fileHash);
    };
  }
};

if (import.meta.main) {
  Deno.exit(await doBuildTask());
}
