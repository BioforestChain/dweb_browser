import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { registryNpmBuilder } from "../../scripts/helper/npmBuilder.ts";
import { npmInit } from "./toolkit-init.ts";

/// 将 import_map.npm.json 和 deno.jsonc 两个文件进行合并
const importMap = await (async () => {
  const { default: importMapNode } = await import("./import_map.npm.json", { with: { type: "json" } });

  const importMapDeno = Function(
    `return(${fs.readFileSync(fileURLToPath(import.meta.resolve("../../deno.jsonc")))})`
  )();
  const importMapMerged = { imports: { ...importMapDeno.imports, ...importMapNode.imports } };
  const importMapJson = JSON.stringify(importMapMerged, null, 2);
  const hash = crypto.createHash("sha256").update(importMapJson).digest("hex").slice(0, 8);
  const filename = path.resolve(os.tmpdir(), `import_map.${hash}.json`);
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
  options: {
    scriptModule: false,
  },
});

export const plaocCli = registryNpmBuilder({
  packageDir: import.meta.resolve("../plaoc/cli/"),
  version,
  importMap,
  entryPointsDirName: false,
  options: {
    scriptModule: false,
    shims: {
      deno: true,
    },
    entryPoints: [
      {
        kind: "bin",
        name: "plaoc",
        path: import.meta.resolve("../plaoc/cli/plaoc.ts"),
      },
    ],
  },
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

export const doBuildNpm = async () => {
  // 首先要确保所有 npm/package.json 在线，否则 plaoc/examples 这类项目就会寻找异常
  await npmInit();
  // 并行编译
  await Promise.all([
    dwebHelper(),
    dwebCore(),
    dwebJsProcess(),
    dwebPolyfill(),
    plaocServer(),
    plaocCli(),
    plaocPlugins(),
    plaocIsDweb(),
  ]);
};

if (import.meta.main) {
  doBuildNpm();
}
