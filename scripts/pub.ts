import { whichSync } from "jsr:@david/which";
import picocolors from "npm:picocolors";
import { cwdResolve } from "./helper/cwd.ts";
import { asyncNpmMirror } from "./sync_npm.ts";

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

  /// 更新package.json配置文件
  const packagePath = cwdResolve(cwd + "/package.json");
  const packageJson = (
    await import(packagePath, {
      with: { type: "json" },
    })
  ).default;
  if (packageJson.version !== config.version) {
    packageJson.version = config.version;
    /// 写入配置文件
    Deno.writeFileSync(
      new URL(packagePath, import.meta.url),
      new TextEncoder().encode(JSON.stringify(packageJson, null, 2)),
      {}
    );
  }
  // 看看需不需要发布版本
  if (await checkVersion(cwd, { name: config.name, version: config.version })) {
    return;
  }
  await doPublish(config.buildToRootDir);
  await asyncNpmMirror(config.name);
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
