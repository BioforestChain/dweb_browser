import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { toolkitInit } from "../toolkit/scripts/toolkit-init.ts";
import { AssetsConfig } from "./helper/AssetsConfig.ts";
import { $ } from "./helper/exec.ts";
import { resolveDenoJson } from "./helper/resolver.ts";

export const doInit = async (args: string[]) => {
  // 初始化 git 项目
  await $(`git submodule update --init`);
  for (const depValue of Object.values(resolveDenoJson().imports)) {
    if (depValue.startsWith("npm:")) {
      await $(`deno cache ${depValue}`);
    }
  }

  await toolkitInit();

  /// 这个 assets 不做等待
  void doInitAssets(args.includes("--watch") || args.includes("--dev"));
};

AssetsConfig.createAndSave("helper-platform-drawable", [
  {
    type: "copyKmpResDrawable",
    moduleName: "helperPlatform",
    ignoreSharedFix: true,
    moduleTarget: "desktopMain",
  },
]);
AssetsConfig.createAndSave("sys-icons", [
  {
    type: "linkKmpResFiles",
    moduleName: "sys",
    ignoreSharedFix: true,
    moduleTarget: "desktopMain",
  },
]);
AssetsConfig.createAndSave("browser-drawable", [
  {
    type: "copyKmpResDrawable",
    moduleName: "browser",
  },
]);
AssetsConfig.createAndSave("browser-icons", [
  {
    type: "linkKmpResFiles",
    moduleName: "browser",
  },
]);

export const doInitAssets = $once(async (watch: boolean) => {
  /// 这里是为了让 toolkitTasks 将 AssetsConfig 写满
  await import("../toolkit/scripts/toolkit-dev.ts");
  await Promise.all(
    [...AssetsConfig.ALL.values()].map((it) => {
      it.effect(watch);
    })
  );
});
if (import.meta.main) {
  doInit(Deno.args);
}
