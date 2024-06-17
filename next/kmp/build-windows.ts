import fs from "node:fs";
import path from "node:path";
import { WalkFiles } from "../../scripts/helper/WalkDir.ts";
import { $ } from "../../scripts/helper/exec.ts";
import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";
import { cliArgs } from "./build-helper.ts";
import { getSuffix } from "./build-macos.ts";
import { doUploadRelease, recordUploadRelease, type $UploadReleaseParams } from "./upload-github-release.ts";
const resolveTo = createBaseResolveTo(import.meta.url);

async function doRelease(suffix: string) {
  $.cd(import.meta.resolve("./"));
  console.info("💡 开始执行编译");
  // -PreleaseBuild=true 增加传入参数表示当前是 release 打包操作
  await $(`${resolveTo("gradlew.bat")} :desktopApp:packageReleaseMsi -PreleaseBuild=true`);

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
  const version = msiFilepath.match(/\d+\.\d+\.\d+/);
  if (version) {
    return {
      version: version[0],
      filepath: msiFilepath,
    };
  }
}

if (import.meta.main) {
  const result = await doRelease(getSuffix());
  if (result) {
    const uploadArgs: $UploadReleaseParams = [`desktop-${result.version}`, result.filepath];
    await recordUploadRelease(`desktop-${result.version}/windows`, uploadArgs);
    if (cliArgs.upload) {
      await doUploadRelease(...uploadArgs);
    }
  }
}
