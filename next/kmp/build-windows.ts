import fs from "node:fs";
import path from "node:path";
import { WalkFiles } from "../../scripts/helper/WalkDir.ts";
import { $ } from "../../scripts/helper/exec.ts";
import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";
import { getSuffix } from "./build-macos.ts";
const resolveTo = createBaseResolveTo(import.meta.url);

async function doRelease(suffix: string) {
  $.cd(import.meta.resolve("./"));
  console.info("💡 开始执行编译");
  await $("./gradlew.bat :desktopApp:packageReleaseMsi");

  const msiDir = resolveTo("./app/desktopApp/build/compose/binaries/main/msi");
  const msiFile = [...WalkFiles(msiDir)].sort((a, b) => b.stats.birthtimeMs - a.stats.birthtimeMs).shift();

  if (!msiFile) {
    console.error("❌ 找不到最终的 msi 文件");
    return;
  }
  let msiFilepath: string;
  if (false === msiFile.entryname.includes("-" + suffix)) {
    const newFilename = msiFile.entryname.replace(".msi", `-${suffix}.msi`);
    fs.renameSync(msiFile.entrypath, (msiFilepath = path.resolve(msiFile.dirpath, newFilename)));
  } else {
    msiFilepath = msiFile.entrypath;
  }
  return msiFilepath;
}

if (import.meta.main) {
  doRelease(getSuffix());
}
