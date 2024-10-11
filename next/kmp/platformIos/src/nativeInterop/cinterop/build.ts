import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

import { walkSync } from "jsr:@std/fs";
import { which } from "jsr:@david/which";
import { doArchiveItemTask } from "./archive.ts";
import { doCreateXcItemTask } from "./create-xc.ts";
import { __dirname, exec, runTasks, sourceCodeDir } from "./util.ts";

export const doBuildTask = async () => {
  // 使用xcodebuild 来判断是否是有 Xcode
  const xcodebuild = await which("xcodebuild");
  if(!xcodebuild) {
    return 0;
  }

  let isExists = false;
  const xcodePaths = ["/Applications/Xcode.app", os.homedir() + "/Applications/Xcode.app"];

  for await (const xcodePath of xcodePaths) {
    isExists = fs.existsSync(xcodePath);
    if (isExists) {
      break;
    }
  }

  let xcode = "/Applications/Xcode.app";
  try {
    const xcodeUrlByte = (await new Deno.Command("xcode-select", { args: ["-p"] }).output()).stdout;
    const xcodeUrl = new TextDecoder().decode(xcodeUrlByte);
    xcode = xcodeUrl.substring(0, xcodeUrl.indexOf("Xcode.app") + "Xcode.app".length);
    isExists = true;
  } catch {
    console.error("you need install x-code!!");
    return 0;
  }
  if (!isExists) {
    return 0;
  }
  console.log("xcodeUrl=>", xcode);
  const fws = ["DwebPlatformIosKit", "DwebWebBrowser"];
  for (const fw of fws) {
    const writeFileHash = calcHash(fw);
    if (!writeFileHash) {
      console.log("build cached!! --> " + fw);
      continue;
    }
    console.log("will build --> " + fw);
    const result = await runTasks(doArchiveItemTask(fw), doCreateXcItemTask(fw), async () => {
      writeFileHash();
      console.log("build success!! --> " + fw);
      return 0;
    });
    if (result !== 0) {
      return result;
    }
  }
  return 0;
};

const calcHash = (fw: string): (() => void) | undefined => {
  const hashBuilder = crypto.createHash("sha256");
  for (const entry of walkSync(path.resolve(sourceCodeDir, fw))) {
    hashBuilder.update(entry.path);
    const stat = fs.statSync(entry.path);
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
  const code = await doBuildTask();
  if (
    // 配置了环境变量
    Deno.env.get("IGNORE_XCODE_BUILD") == "true" ||
    // 70 代表开发者环境问题，强制跳过
    code === 70
  ) {
    Deno.exit(0);
  } else {
    Deno.exit(code);
  }
}
