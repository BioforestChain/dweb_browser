import { WalkFiles } from "../../scripts/helper/WalkDir.ts";
import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";
const resolveTo = createBaseResolveTo(import.meta.url);

// 发布版本的时候，升级下版本信息 versionCode和versionName
upgradeVersionInfo(resolveTo("gradle/libs.versions.toml"))

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

// 异步读取并升级版本信息
async function upgradeVersionInfo(filePath: string) {
  try {
    // 读取文件内容
    const content = await Deno.readTextFile(filePath);
    
    // 将文件内容按行分割
    const lines = content.split("\r\n");

    // 遍历每一行，判断是否是 versionCode和versionName，然后进行修改
    for(let index = 0; index < lines.length; index++) {
      const line = lines[index]
      if(line.startsWith("versionCode")) { // 修改 versionCode
        const versionCode = line.substring(line.indexOf("=") + 1).replaceAll("\"", "").trim()
        lines[index] = `versionCode = "${parseInt(versionCode) + 1}"`
        console.log(`versionCode ${versionCode} >> ${lines[index]}`)
      } else if(line.startsWith("versionName")) { // 修改 versionName
        const now = new Date()
        const currentDate = `${now.getFullYear()-2000}${(now.getMonth()+1).toString().padStart(2, '0')}${(now.getDate()).toString().padStart(2, '0')}`
        const versionName = line.substring(line.indexOf("=") + 1).replaceAll("\"", "").trim().split(".")
        if(versionName[1] === currentDate) {
          lines[index] = `versionName = "${versionName[0]}.${versionName[1]}.${parseInt(versionName[2]) + 1}"`
        } else {
          lines[index] = `versionName = "${versionName[0]}.${currentDate}.0"`
        }
        console.log(`versionName ${versionName} >> ${lines[index]}`)
        break
      }
    }

    await Deno.writeTextFile(filePath, lines.join("\r\n"))
    console.log("版本更新完成，开始打包...")
  } catch (error) {
    console.error("版本更新失败:", error);
  }
}