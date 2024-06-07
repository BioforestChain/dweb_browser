import fs from "node:fs";
import { WalkFiles } from "../../scripts/helper/WalkDir.ts";
import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";
const resolveTo = createBaseResolveTo(import.meta.url);

// 运行 assembleRelease 命令，继承输出
const cmd = Deno.build.os === "windows" ? resolveTo("gradlew.bat") : resolveTo("gradlew");
const args = ["assembleRelease", "assembleDebug", "bundleRelease"];
const doBundle = async () => {
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
};

// 定义APK目标目录
const OUTPUT_DIR = resolveTo("./app/androidApp/release");

// // 创建、清空目标目录
// Deno.removeSync(OUTPUT_DIR, { recursive: true });
// Deno.mkdirSync(OUTPUT_DIR, { recursive: true });

const doCopy = async (versionName: string) => {
  const CONDITIONS = ["debug", "release", "for"];
  const outputDir = `${OUTPUT_DIR}/android_v${versionName}`;
  // 创建目标目录
  if (fs.existsSync(outputDir)) {
    Deno.removeSync(outputDir, { recursive: true });
  }
  Deno.mkdirSync(outputDir, { recursive: true });

  const copyFile = async (baseDir: string, dirEntry: Deno.DirEntry) => {
    if (dirEntry.isDirectory) {
      const variant = dirEntry.name;
      if (CONDITIONS.some((cond) => variant.startsWith(cond))) {
        console.log(`Processing ${variant}...`);
        const variantPath = `${baseDir}/${variant}`;

        for await (const fileEntry of WalkFiles(variantPath)) {
          if (fileEntry.entryname.endsWith(".apk") || fileEntry.entryname.endsWith(".aab")) {
            const version = fileEntry.entryname.match(/v(\d+\.\d+\.\d+)/)?.at(1);
            if (version == versionName) {
              console.log(`  Copying ${fileEntry.relativepath}`);

              await Deno.copyFile(fileEntry.entrypath, `${outputDir}/${fileEntry.entryname}`);
            }
          }
        }
      }
    }
  };
  // 遍历构建输出目录，筛选需要的文件夹
  const BUILD_OUTPUT_DIR = resolveTo("./app/androidApp/build/outputs/apk");
  for await (const dirEntry of Deno.readDir(BUILD_OUTPUT_DIR)) {
    await copyFile(BUILD_OUTPUT_DIR, dirEntry);
  }

  // 遍历构建输出目录，筛选需要的文件夹(针对aab文件)
  const BUILD_OUTPUT_DIR_BUNDLE = resolveTo("./app/androidApp/build/outputs/bundle");
  for await (const dirEntry of Deno.readDir(BUILD_OUTPUT_DIR_BUNDLE)) {
    await copyFile(BUILD_OUTPUT_DIR_BUNDLE, dirEntry);
  }

  console.log("output dir", outputDir);
};

console.log(`All required APKs have been copied to ${OUTPUT_DIR}`);

// 异步读取并升级版本信息
const upgradeVersionInfo = async (filePath: string, forceUpdate = false) => {
  try {
    // 读取文件内容
    const content = fs.readFileSync(filePath, "utf-8");

    // 将文件内容按行分割
    const lines = content.split("\n");
    const versionCode = lines.find((line) => line.startsWith("versionCode ="))!.match(/\d+/)![0];
    const versionName = lines.find((line) => line.startsWith("versionName ="))!.match(/[\d\.]+/)![0];

    let newVersionName = versionName;
    let newVersionCode = versionCode;

    const versionParts = versionName.split(".");
    const now = new Date();
    const currentVersionDate = [now.getFullYear() - 2000, now.getMonth() + 1, now.getDate()]
      .map((it) => it.toString().padStart(2, "0"))
      .join("");
    // 如果日期版本号不一致，那么就进行更新
    if (currentVersionDate !== versionParts[1]) {
      newVersionName = [versionParts[0], currentVersionDate, "0"].join(".");
    }
    // 如果需要，强制升级小版本
    else if (forceUpdate) {
      newVersionName = [versionParts[0], currentVersionDate, parseInt(versionParts[2]) + 1].join(".");
    }

    if (newVersionName != versionName) {
      newVersionCode = (parseInt(versionCode) + 1).toString();
      fs.writeFileSync(
        filePath,
        lines
          .map((line) => {
            if (line.startsWith("versionCode =")) {
              return line.replace(/\d+/, newVersionCode);
            } else if (line.startsWith("versionName =")) {
              return line.replace(/[\d\.]+/, newVersionName);
            }
            return line;
          })
          .join("\n")
      );
      console.log("versionCode =", newVersionCode);
      console.log("versionName =", newVersionName);
      console.log("版本更新，开始打包...");
    } else {
      console.log("versionCode =", newVersionCode);
      console.log("versionName =", newVersionName);
      console.log("版本保持，开始打包...");
    }
    return newVersionName;
  } catch (error) {
    console.error("版本更新失败:", error);
    Deno.exit(1);
  }
};

// 发布版本的时候，升级下版本信息 versionCode和versionName
const versionName = await upgradeVersionInfo(resolveTo("gradle/libs.versions.toml"), Deno.args.includes("--new"));
await doBundle();
await doCopy(versionName);
