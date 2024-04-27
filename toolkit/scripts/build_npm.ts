import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { npmBuilder } from "../../scripts/helper/npmBuilder.ts";

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

const version = Deno.args[0];
const once = <R>(fun: () => Promise<R>) => {
  let res: Promise<R> | undefined;
  return () => (res ??= fun());
};

export const dwebHelper = once(async () => {
  await npmBuilder({
    packageDir: import.meta.resolve("../dweb-helper/"),
    version,
    importMap,
  });
});
export const dwebPolyfill = once(async () => {
  await npmBuilder({
    packageDir: import.meta.resolve("../dweb-polyfill/"),
    version,
    importMap,
  });
});

export const dwebCore = once(async () => {
  await dwebHelper();
  await npmBuilder({
    packageDir: import.meta.resolve("../dweb-core/"),
    version,
    importMap,
  });
});
export const dwebJsProcess = once(async () => {
  await npmBuilder({
    packageDir: import.meta.resolve("../dweb-js-process-assets/"),
    version,
    importMap,
    entryPointsDirName: "./worker",
  });
});

export const plaocServer = once(async () => {
  await npmBuilder({
    packageDir: import.meta.resolve("../plaoc/server/"),
    version,
    importMap,
    entryPointsDirName: false,
    options: {
      scriptModule: false,
    },
  });
});

export const plaocCli = once(async () => {
  await npmBuilder({
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
});
export const plaocPlugins = once(async () => {
  await npmBuilder({
    packageDir: import.meta.resolve("../plaoc/plugins/"),
    version,
    importMap,
    entryPointsDirName: false,
  });
});
export const plaocIsDweb = once(async () => {
  await npmBuilder({
    packageDir: import.meta.resolve("../plaoc/is-dweb/"),
    version,
    importMap,
    entryPointsDirName: false,
  });
});

if (import.meta.main) {
  //   void dwebHelper();
  //   void dwebPolyfill();
  //   void dwebCore();
  //   void dwebJsProcess();
  //   void plaocServer();
  //   void plaocCli();
  void plaocPlugins();
  //   void plaocIsDweb();
}
