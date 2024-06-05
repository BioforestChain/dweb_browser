import { WalkFiles } from "../../scripts/helper/WalkDir.ts";
import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";
const resolveTo = createBaseResolveTo(import.meta.url);

// 运行 assembleRelease 命令，继承输出
const cmd = Deno.build.os === "windows" ? resolveTo("gradlew.bat") : resolveTo("gradlew");
const args = ["assembleRelease", "assembleDebug", "bundleRelease"];
console.log(">", cmd, ...args);

// 执行 assembleRelease
const gradle = new Deno.Command(cmd, {
  cwd: resolveTo(),
  args: args,
  stdout: "inherit",
  stderr: "inherit",
});

// 等待命令完成
const { code } = await gradle.spawn().status;
if (code !== 0) {
  console.error(`${cmd} 失败`);
  Deno.exit(code);
}

// 定义APK目标目录
const OUTPUT_DIR = resolveTo("./app/androidApp/release");
const BUILD_OUTPUT_DIR = resolveTo("./app/androidApp/build/outputs/apk");
const CONDITIONS = ["debug", "release", "with"];

// // 创建、清空目标目录
// Deno.removeSync(OUTPUT_DIR, { recursive: true });
// Deno.mkdirSync(OUTPUT_DIR, { recursive: true });

// 遍历构建输出目录，筛选需要的文件夹
for await (const dirEntry of Deno.readDir(BUILD_OUTPUT_DIR)) {
  if (dirEntry.isDirectory) {
    const variant = dirEntry.name;
    if (CONDITIONS.some((cond) => variant.startsWith(cond))) {
      console.log(`Processing ${variant}...`);
      const variantPath = `${BUILD_OUTPUT_DIR}/${variant}`;

      for await (const fileEntry of WalkFiles(variantPath)) {
        if (fileEntry.entryname.endsWith(".apk")) {
          const version = fileEntry.entryname.match(/v\d+\.\d+\.\d+/);
          console.log(`  Copying ${fileEntry.relativepath}`);
          const outputDir = `${OUTPUT_DIR}/${version}`;
          // 创建、清空目标目录
          Deno.mkdirSync(outputDir, { recursive: true });
          Deno.mkdirSync(outputDir, { recursive: true });

          await Deno.copyFile(fileEntry.entrypath, `${outputDir}/${fileEntry.entryname}`);
        }
      }
    }
  }
}

// 遍历构建输出目录，筛选需要的文件夹(针对aab文件)
const BUILD_OUTPUT_DIR_BUNDLE = resolveTo("./app/androidApp/build/outputs/bundle");
for await (const dirEntry of Deno.readDir(BUILD_OUTPUT_DIR_BUNDLE)) {
  if (dirEntry.isDirectory) {
    const variant = dirEntry.name;
    if (CONDITIONS.some((cond) => variant.startsWith(cond))) {
      console.log(`Processing ${variant}...`);
      const variantPath = `${BUILD_OUTPUT_DIR_BUNDLE}/${variant}`;

      for await (const fileEntry of WalkFiles(variantPath)) {
        if (fileEntry.entryname.endsWith(".aab")) {
          const version = fileEntry.entryname.match(/v\d+\.\d+\.\d+/);
          console.log(`  Copying ${fileEntry.relativepath}`);
          const outputDir = `${OUTPUT_DIR}/android_${version}`;
          // 创建、清空目标目录
          Deno.mkdirSync(outputDir, { recursive: true });
          Deno.mkdirSync(outputDir, { recursive: true });

          await Deno.copyFile(fileEntry.entrypath, `${outputDir}/${fileEntry.entryname}`);
        }
      }
    }
  }
}

console.log(`All required APKs have been copied to ${OUTPUT_DIR}`);
