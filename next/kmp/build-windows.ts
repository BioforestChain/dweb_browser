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
  if (false === msiFile.entryname.includes("-" + suffix)) {
    msiFile.entryname.replace(".msi", `-${suffix}.msi`);
  }
}

if (import.meta.main) {
  doRelease(getSuffix());
}
