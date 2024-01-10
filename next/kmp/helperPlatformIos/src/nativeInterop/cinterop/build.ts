import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

import { WalkFiles } from "../../../../../../plaoc/cli/helper/walk-dir.ts";
import { which } from "../../../../../../scripts/helper/WhichCommand.ts";
import { doArchiveItemTask } from "./archive.ts";
import { doCreateXcItemTask } from "./create-xc.ts";
import { __dirname, sourceCodeDir, runTasks } from "./util.ts";

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

    const fws = ["DwebPlatformIosKit", "DwebWebBrowser"]
    for (const fw of fws) {
      const writeFileHash = calcHash(fw);
      if (!writeFileHash) {
        console.log("build cached!! --> " + fw);
        continue;
      }
      console.log("will build --> " + fw);
      await runTasks(
        doArchiveItemTask(fw),
        doCreateXcItemTask(fw),
        async () => {
          writeFileHash();
          console.log("build success!! --> " + fw);
          return 0;
        }
      );
    }
    return 0
};

const calcHash = (fw: string): (() => void) | undefined => {
  const hashBuilder = crypto.createHash("sha256");
  for (const entry of WalkFiles(path.resolve(sourceCodeDir, "./" + fw))) {
    hashBuilder.update(entry.entrypath);
    const stat = fs.statSync(entry.entrypath);
    hashBuilder.update(JSON.stringify([stat.size, stat.ctimeMs, stat.mtimeMs]));
  }

  const fileHash = hashBuilder.digest("hex");

  const buildCacheHash = path.resolve(__dirname, "./archives/" + fw + "-build-cache.temp");
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
