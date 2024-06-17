import { DOMParser } from "https://deno.land/x/deno_dom/deno-dom-wasm.ts";
import fs from "node:fs";
import os from "node:os";
import { $ } from "../../scripts/helper/exec.ts";

import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";
import { cliArgs, localProperties } from "./build-helper.ts";
import { doUploadRelease, recordUploadRelease, type $UploadReleaseParams } from "./upload-github-release.ts";

const resolveTo = createBaseResolveTo(import.meta.url);

const notarization_appleId = localProperties.get("compose.desktop.mac.notarization.appleID");
const notarization_password = localProperties.get("compose.desktop.mac.notarization.password");
const notarization_teamID = localProperties.get("compose.desktop.mac.notarization.teamID");

export const suffixMap = new Map<$Arch, string>([
  ["x64", "x86_64"],
  ["ia32", "x86"],
  ["arm", "arm32"],
]);
export type $Arch = "arm" | "arm64" | "ia32" | "mips" | "mipsel" | "ppc" | "ppc64" | "s390" | "s390x" | "x64";
export const getSuffix = (_arch: string = cliArgs.arch) => {
  const arch = (_arch ?? os.arch()) as $Arch;
  const suffix = suffixMap.get(arch) ?? arch;
  return suffix;
};

async function doRelease(suffix: string) {
  if (false === localProperties.getBoolean("compose.desktop.mac.sign")) {
    console.error("❌ 缺少 compose.desktop.mac.sign 配置，无法正确执行打包");
    return;
  }

  $.cd(import.meta.resolve("./"));
  console.info("💡 开始执行编译");
  // -PreleaseBuild=true 增加传入参数表示当前是 release 打包操作
  await $("./gradlew :desktopApp:createReleaseDistributable -PreleaseBuild=true");

  return await doNotarization(suffix);
}

async function doNotarization(suffix: string) {
  const canNotarization = notarization_appleId && notarization_password && notarization_teamID;
  if (!canNotarization) {
    console.warn("❌ 缺少 notarization 配置，将无法自动执行公证任务");
    return;
  }
  const appleId = notarization_appleId;
  const password = notarization_password;
  const teamID = notarization_teamID;

  console.info("💡 开始执行公证任务");

  await $.cd(import.meta.resolve("./app/desktopApp/build/compose/binaries/main-release"));

  await $(`mv app app-${suffix}`);
  await $(`/usr/bin/ditto -c -k app-${suffix}/DwebBrowser.app zip/DwebBrowser-${suffix}.zip`);

  let submissionId: string | undefined;
  let submissionStatus: string | undefined;
  await $(
    [
      `/usr/bin/xcrun`,
      `notarytool`,
      `submit`,
      `--wait`,
      `--apple-id`,
      appleId,
      "--team-id",
      teamID,
      "--password",
      password,
      `zip/DwebBrowser-${suffix}.zip`,
    ],
    undefined,
    {
      onStdout: (log) => {
        for (const line of log.split("\n")) {
          if (line.includes("id:")) {
            submissionId = line.split(":").pop()?.trim();
            console.log(`✅ 取得 submissionId=${submissionId}`);
          } else if (line.includes("status:")) {
            submissionStatus = line.split(":").pop()?.trim();
            console.log(`✅ 取得 submissionStatus=${submissionStatus}`);
          }
        }
      },
    }
  );
  if (submissionStatus !== "Accepted") {
    if (submissionId) {
      await $([
        `xcrun`,
        `notarytool`,
        `log`,
        submissionId,
        `--apple-id`,
        appleId,
        `--team-id`,
        teamID,
        `--password`,
        password,
      ]);
    } else {
      throw new Error(`submit error: ${submissionStatus}, no found submissionId`);
    }
  } else {
    console.info("💡 开始执行打包");
    await $([`/usr/bin/xcrun`, `stapler`, `staple`, `app-${suffix}/DwebBrowser.app`]);
    const version = getVersion(suffix);

    const dmgFilename = `DwebBrowser-${version}-${suffix}.dmg`;
    await $([
      `create-dmg`,
      `--volname`,
      "Dweb Browser Installer",
      "--volicon",
      "../../../../src/desktopMain/res/icons/mac/icon.icns",
      "--window-pos",
      `200`,
      `120`,
      "--window-size",
      "800",
      "400",
      `--icon-size`,
      "100 ",
      `--icon`,
      "DwebBrowser.app",
      "200",
      `190`,
      `--hide-extension`,
      "DwebBrowser.app",
      `--app-drop-link`,
      `600`,
      `185`,
      dmgFilename,
      `app-${suffix}`,
    ]);

    const dmgFilepath = resolveTo($.pwd(), dmgFilename);
    console.log("✅ 构建完成:", dmgFilepath);
    return { version: version, filepath: dmgFilepath };
  }
}

const getVersion = (suffix: string) => {
  const plist = fs.readFileSync(
    resolveTo(`./app/desktopApp/build/compose/binaries/main-release/app-${suffix}/DwebBrowser.app/Contents/Info.plist`),
    "utf-8"
  );
  const result = new DOMParser().parseFromString(plist, "text/html");
  for (const dict of result.getElementsByTagName("dict")) {
    let ele = dict.firstElementChild;
    while (ele) {
      if (ele.tagName === "KEY" && ele.textContent === "CFBundleShortVersionString") {
        return ele.nextElementSibling!.textContent;
      }
      ele = ele.nextElementSibling;
    }
  }
  throw new Error("No found version string");
};

if (import.meta.main) {
  // 如果有手动指明 arch，那么不做编译，只做分发
  if (cliArgs.arch) {
    await doNotarization(getSuffix());
  } else {
    const result = await doRelease(getSuffix());
    if (result && cliArgs.upload) {
      const uploadArgs: $UploadReleaseParams = [`desktop-${result.version}`, result.filepath];
      await recordUploadRelease(`desktop-${result.version}/macos`, uploadArgs);
      if (cliArgs.upload) {
        await doUploadRelease(...uploadArgs);
      }
    }
  }
}
