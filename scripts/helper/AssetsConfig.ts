import { debounce } from "@dweb-browser/helper/decorator/$debounce.ts";
import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { mapHelper } from "@dweb-browser/helper/fun/mapHelper.ts";
import { copySync } from "@std/fs/copy";
import { emptyDirSync } from "@std/fs/empty-dir";
import fs from "node:fs";
import os from "node:os";
import { default as node_path } from "node:path";
import { $ } from "./exec.ts";
import { createBaseResolveTo } from "./resolveTo.ts";

const rootResolve = createBaseResolveTo(import.meta.resolve("../../"));
const assetsResolve = createBaseResolveTo(rootResolve("./assets"));
const kmpResolve = createBaseResolveTo(rootResolve("./next/kmp"));
export type UseAssets =
  | {
      type: "linkKmpResFiles";
      moduleName: string;
      moduleTarget?: string;
      alias?: string;
      ignoreSharedFix?: boolean;
      sharedFixTarget?: string;
    }
  | {
      type: "copyKmpResDrawable";
      moduleName: string;
      moduleTarget?: string;
      filter?: string;
      ignoreSharedFix?: boolean;
      sharedFixTarget?: string;
    };
export class AssetsConfig {
  static ALL = new Map<string, AssetsConfig>();
  static createAndSave(assetsName: string, useAssets: UseAssets[], assetsDirname?: string) {
    const config = new AssetsConfig(assetsName, useAssets, assetsDirname);
    AssetsConfig.ALL.set(assetsName, config);
    return config;
  }
  constructor(
    readonly assetsName: string,
    readonly useAssets: UseAssets[],
    readonly assetsDirname: string = assetsResolve(assetsName)
  ) {}
  effect(watch: boolean) {
    fs.mkdirSync(this.assetsDirname, { recursive: true });
    return Promise.all(this.useAssets.map((use) => effectUse(this, use, watch)));
  }
}

const symlink = (target: string, dest: string) => {
  if (fs.existsSync(dest)) {
    let isSymbolicLink = false;
    try {
      isSymbolicLink = fs.statSync(dest).isSymbolicLink();
    } catch {}
    if (!isSymbolicLink) {
      // throw new Error(`symbol link fail, ${path} exists.`);
      fs.unlinkSync(dest);
    } else {
      fs.rmSync(dest, { recursive: true }); // 删除文件夹以及其内容
    }
  } else {
    fs.mkdirSync(node_path.dirname(dest), { recursive: true });
  }

  // windows系统需要使用junction模式，否则会有权限问题
  if (os.platform() === "win32") {
    fs.symlinkSync(target, dest, "junction");
  } else {
    fs.symlinkSync(target, dest);
  }
};
const drawableEmptyer = new Map<string, ReturnType<typeof $once>>();
const emptyOnce = (dirname: string) =>
  mapHelper.getOrPut(drawableEmptyer, dirname, () => $once(() => emptyDirSync(dirname)));
const copyDir = (target: string, path: string) => {
  emptyOnce(path)();
  copySync(target, path, { overwrite: true, preserveTimestamps: true });
};

const generateComposeResClass = debounce(async () => {
  await $([os.platform().startsWith("win") ? "./gradlew.bat" : "./gradlew", `generateComposeResClass`], {
    cwd: kmpResolve(),
  });
}, 350);

const effectUse = async (assetConfig: AssetsConfig, use: UseAssets, watch: boolean) => {
  switch (use.type) {
    case "linkKmpResFiles": {
      const filesDirName = use.alias ?? assetConfig.assetsName;
      const moduleTarget = use.moduleTarget ?? "commonMain";
      const sharedTarget = use.sharedFixTarget ?? "iosMain";
      symlink(
        assetConfig.assetsDirname,
        kmpResolve(`${use.moduleName}/src/${moduleTarget}/composeResources/files/${filesDirName}`)
      );
      // desktop 需要 把文件放一份在 resource 中，否则单元测试需要文件的时候，无法找到
      symlink(assetConfig.assetsDirname, kmpResolve(`${use.moduleName}/src/${moduleTarget}/resources/${filesDirName}`));

      // link 到 shared/ios 中
      // TODO 目前只有 ios 存在这个 composeResources 不能多模块的问题，未来可以移除掉
      if (use.ignoreSharedFix !== true) {
        symlink(
          assetConfig.assetsDirname,
          kmpResolve(`shared/src/${sharedTarget}/composeResources/files/${filesDirName}`)
        );
      }
      // files 文件夹不需要做 generate Res
      // void generateComposeResClass();
      break;
    }
    case "copyKmpResDrawable": {
      const moduleTarget = use.moduleTarget ?? "commonMain";
      const sharedTarget = use.sharedFixTarget ?? "iosMain";
      const doCopyDrawable = () => {
        copyDir(
          assetConfig.assetsDirname,
          kmpResolve(`${use.moduleName}/src/${moduleTarget}/composeResources/drawable/`)
        );
        // copy 到 shared 中
        // TODO 目前只有 ios 存在这个 composeResources 不能多模块的问题，未来可以移除掉
        if (use.ignoreSharedFix !== true) {
          copyDir(assetConfig.assetsDirname, kmpResolve(`shared/src/${sharedTarget}/composeResources/drawable/`));
        }
        void generateComposeResClass();
      };
      if (watch) {
        const watchCopyDrawable = debounce(doCopyDrawable, 200);
        watchCopyDrawable();
        for await (const _event of Deno.watchFs(assetConfig.assetsDirname, { recursive: true })) {
          watchCopyDrawable();
        }
      } else {
        doCopyDrawable();
      }
      break;
    }
  }
};
