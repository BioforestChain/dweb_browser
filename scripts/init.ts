import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { toolkitInit } from "../toolkit/scripts/toolkit-init.ts";
import { AssetsConfig } from "./helper/AssetsConfig.ts";
import { $ } from "./helper/exec.ts";
import { resolveDenoJson, rootResolve } from "./helper/resolver.ts";

export const doInit = async (args: string[]) => {
  // 初始化 git 项目
  await $(`git submodule update --init`);
  const npmDeps = Object.values(resolveDenoJson().imports).filter((it) => it.startsWith("npm:"));
  await $(`deno cache ${npmDeps.join(" ")}`);

  await toolkitInit();

  /// 这个 assets 不做等待
  void doInitAssets(args.includes("--watch") || args.includes("--dev"));
};

AssetsConfig.createAndSave(
  "sys-drawable",
  [
    {
      type: "copyKmpResDrawable",
      moduleName: "sys",
      ignoreSharedFix: true,
      moduleTarget: "desktopMain",
    },
  ],
  undefined
);
AssetsConfig.createAndSave(
  "sys-icons",
  [
    {
      type: "linkKmpResFiles",
      moduleName: "sys",
      ignoreSharedFix: true,
      moduleTarget: "desktopMain",
    },
  ],
  undefined
);
AssetsConfig.createAndSave(
  "browser-drawable",
  [
    {
      type: "copyKmpResDrawable",
      moduleName: "browser",
    },
  ],
  undefined
);
AssetsConfig.createAndSave(
  "browser-html5test",
  [
    {
      type: "linkKmpResFiles",
      moduleName: "browser",
    },
  ],
  rootResolve("./toolkit/dweb_html5test")
);
AssetsConfig.createAndSave(
  "browser-icons",
  [
    {
      type: "linkKmpResFiles",
      moduleName: "browser",
    },
  ],
  undefined
);
AssetsConfig.createAndSave(
  "window-drawable",
  [
    {
      type: "copyKmpResDrawable",
      moduleName: "window",
    },
  ],
  undefined
);

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
