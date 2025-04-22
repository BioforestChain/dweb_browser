import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import node_path from "node:path";
import { registryNpmBuilder } from "../../scripts/helper/npmBuilder.ts";
import { resolveDenoJson } from "../../scripts/helper/resolver.ts";
import { npmInit } from "./toolkit-init.ts";
/// 将 import_map.npm.json 和 deno.jsonc 两个文件进行合并
const importMap = await (async () => {
  const { default: importMapNode } = await import("./import_map.npm.json", { with: { type: "json" } });

  const importMapDeno = resolveDenoJson();
  const importMapMerged = { imports: { ...importMapDeno.imports, ...importMapNode.imports } };
  const importMapJson = JSON.stringify(importMapMerged, null, 2);
  const hash = crypto.createHash("sha256").update(importMapJson).digest("hex").slice(0, 8);
  const filename = node_path.resolve(os.tmpdir(), `import_map.${hash}.json`);
  fs.writeFileSync(filename, importMapJson);
  console.log("import_map:", filename);
  return filename;
})();

const version = Deno.args.find((it) => it.match(/^[\d\.]+$/));

export const dwebHelper = registryNpmBuilder({
  packageDir: import.meta.resolve("../dweb-helper/"),
  version,
  importMap,
});
export const dwebPolyfill = registryNpmBuilder({
  packageDir: import.meta.resolve("../dweb-polyfill/"),
  version,
  importMap,
});

export const dwebCore = registryNpmBuilder({
  packageDir: import.meta.resolve("../dweb-core/"),
  version,
  importMap,
});
export const dwebJsProcess = registryNpmBuilder({
  packageDir: import.meta.resolve("../dweb-js-process-assets/"),
  version,
  importMap,
  entryPointsDirName: "./worker",
});

export const plaocServer = registryNpmBuilder({
  packageDir: import.meta.resolve("../plaoc/server/"),
  version,
  importMap,
  entryPointsDirName: false,
  emptyDirIgnore: (name) => {
    return name === "dist";
  },
  options: (ctx) => ({
    scriptModule: false,
    entryPoints: [
      { name: ".", path: ctx.packageResolve("./index.ts") },
      { name: "./middlewares", path: ctx.packageResolve("./middlewares/index.ts") },
    ],
    postBuild: () => {
      try {
        Deno.statSync(ctx.packageResolve("./dist"));
      } catch {
        Deno.symlinkSync(ctx.npmResolve("./dist"), ctx.packageResolve("./dist"), { type: "junction" });
      }
    },
  }),
});
//

export const plaocCli = registryNpmBuilder({
  packageDir: import.meta.resolve("../plaoc/cli/"),
  version,
  importMap,
  entryPointsDirName: false,
  options: (ctx) => ({
    typeCheck: "both",
    scriptModule: false,
    shims: {
      deno: true,
      webSocket: true,
    },
    mappings: {
      "./toolkit/plaoc/cli/platform/plaocServer.deno.ts": "./toolkit/plaoc/cli/platform/plaocServer.node.ts",
      "./toolkit/plaoc/cli/platform/initWasm.deno.ts": "./toolkit/plaoc/cli/platform/initWasm.node.ts",
    },
    entryPoints: [
      {
        kind: "bin",
        name: "plaoc",
        path: import.meta.resolve("../plaoc/cli/plaoc.ts"),
      },
    ],
    filterDiagnostic(diagnostic) {
      const fileName = diagnostic.file?.fileName;
      if (fileName?.endsWith("_generic_list.ts") || fileName?.endsWith("provider.ts")) {
        return false; // ignore all diagnostics in this file
      }
      // etc... more checks here
      return true;
    },
    postBuild: () => {
      Deno.copyFileSync(ctx.packageResolve("./ws/certificate.crt"), ctx.npmResolve("./esm/ws/certificate.crt"));
      Deno.copyFileSync(ctx.packageResolve("./ws/private.key"), ctx.npmResolve("./esm/ws/private.key"));
    },
  }),
});
export const plaocPlugins = registryNpmBuilder({
  packageDir: import.meta.resolve("../plaoc/plugins/"),
  version,
  importMap,
  entryPointsDirName: false,
});
export const plaocIsDweb = registryNpmBuilder({
  packageDir: import.meta.resolve("../plaoc/is-dweb/"),
  version,
  importMap,
  entryPointsDirName: false,
});

export const dwebTranslate = registryNpmBuilder({
  packageDir: import.meta.resolve("../dweb-utils/dweb-translate/"),
  version,
  importMap,
  entryPointsDirName: false,
  options: {
    typeCheck: "both",
    scriptModule: false,
    shims: {
      deno: true,
    },
    entryPoints: [
      {
        kind: "bin",
        name: "dwebt",
        path: import.meta.resolve("../dweb-utils/dweb-translate/index.ts"),
      },
    ],
  },
});

export const dwebSign = registryNpmBuilder({
  packageDir: import.meta.resolve("../dweb-utils/dweb-sign/"),
  version,
  importMap,
});

export const doBuildNpm = async () => {
  // 首先要确保所有 npm/package.json 在线，否则 plaoc/examples 这类项目就会寻找异常
  await npmInit();
  // 并行编译
  await dwebHelper();
  await dwebCore();
  await dwebJsProcess();
  await Promise.all([
    dwebPolyfill(),
    plaocServer(),
    plaocCli(),
    plaocPlugins(),
    plaocIsDweb(),
    dwebTranslate(),
    dwebSign(),
  ]);
};

if (import.meta.main) {
  doBuildNpm();
}
