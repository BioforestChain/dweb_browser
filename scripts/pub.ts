import picocolors from "npm:picocolors";
import { whichSync } from "./helper/WhichCommand.ts";
import { cwdResolve } from "./helper/cwd.ts";

/**检查版本 */
export const checkVersion = async (cwd: string, target: { name: string; version: string }) => {
  const cmdWhich = whichSync("npm")!;
  // #region 检查版本
  const packageJson = target;
  const output = await new Deno.Command(cmdWhich, {
    cwd,
    args: ["info", packageJson.name, "--json"],
    stdout: "piped",
  })
    .spawn()
    .output();
  const packageInfo = JSON.parse(new TextDecoder().decode(output.stdout));
  if (packageInfo.versions.includes(packageJson.version)) {
    console.warn(
      picocolors.yellow(
        `package ${picocolors.cyan(packageJson.name)}${picocolors.dim("@")}${picocolors.green(
          packageJson.version
        )} already exists`
      )
    );
    return true;
  }
  return false;
};

export const doPublish = async (cwd: string) => {
  const cmdWhich = whichSync("pnpm")!;
  // #regionend
  const npm_cmd = new Deno.Command(cmdWhich, {
    cwd,
    args: ["publish", "--access", "public", "--no-git-checks"],
    stdout: "inherit",
    stderr: "inherit",
    stdin: "inherit",
  });
  const process = npm_cmd.spawn();
  const status = await process.status;
  return status.success;
};

/**
 * 将 publish.json中的版本同步到package.json
 */
export const doUpdatePackage = async (inputConfigFile: string, target: string) => {
  const npmConfigs: Packages = (await import(inputConfigFile, { with: { type: "json" } })).default;
  const config = npmConfigs[target];
  const cwd = config.buildToRootDir;
  // 看看需不需要更新版本
  if (await checkVersion(cwd, { name: config.name, version: config.version })) {
    return;
  }
  /// 更新package.json配置文件
  const packagePath = cwdResolve(cwd + "/package.json");
  const packageJson = (
    await import(packagePath, {
      with: { type: "json" },
    })
  ).default;
  packageJson.version = config.version;
  /// 写入配置文件
  Deno.writeFileSync(
    new URL(packagePath, import.meta.url),
    new TextEncoder().encode(JSON.stringify(packageJson, null, 2)),
    {}
  );
  await doPublish(config.buildToRootDir);
  await asyncNpmMirror(config.name);
};
const SYNCS_NPM_NIRROR = "https://registry-direct.npmmirror.com/-/package/";
const asyncNpmMirror = async (name: string) => {
  const path = SYNCS_NPM_NIRROR + `${name}/syncs`;
  const res = await fetch(path, { method: "PUT" });
  const result = await res.json();
  if (result.ok) {
    console.log("🏆npm_nirror 镜像站同步成功", `状态：${result.state}`);
  } else {
    console.log("💢npm_nirror 同步失败", result);
  }
};

export const doPub = async (args = Deno.args) => {
  // 请勿随意更改发包顺序，发包只需要更改publish.json里的版本
  const targets =
    args.length === 0 ? ["helper", "core", "js-process", "server", "plugins", "cli", "is-dweb", "polyfill"] : args;
  for (const target of targets) {
    console.log(picocolors.blue(`start pub ${picocolors.cyan(target)}:`));
    await doUpdatePackage(import.meta.resolve(`./publish.json`), target);
  }
};

if (import.meta.main) {
  doPub();
}

interface PackageInfo {
  buildToRootDir: string;
  name: string;
  version: string;
}

interface Packages {
  [key: string]: PackageInfo;
}
