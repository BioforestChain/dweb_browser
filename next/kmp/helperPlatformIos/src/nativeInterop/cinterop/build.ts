import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

import { WalkFiles } from "../../../../../../plaoc/cli/helper/walk-dir.ts";
import { which } from "../../../../../../scripts/helper/WhichCommand.ts";
import { doArchiveTask } from "./archive.ts";
import { doCreateXcTask } from "./create-xc.ts";
import { __dirname, runTasks } from "./util.ts";

export const doBuildTask = async () => {
  const xcodebuild = await which("xcodebuild");

  if (!xcodebuild) {
    Deno.exit(0);
  }

  let isExists = false;
  const xcodePaths = ["/Applications/Xcode.app", os.homedir() + "/Applications/Xcode.app"];

  for await (const xodePath of xcodePaths) {
    isExists = fs.existsSync(xodePath);
    if (isExists) {
      break;
    }
  }

  if (!isExists) {
    Deno.exit(0);
  }

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
