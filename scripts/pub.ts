import picocolors from "npm:picocolors";
import { whichSync } from "./helper/WhichCommand.ts";
import { cwdResolve } from "./helper/cwd.ts";

export const doPubCore = async (cwd: string, autoCheck = false) => {
  const cmdWhich = whichSync("npm")!;
  // #region 检查版本
  if (autoCheck) {
    const packageJson = (
      await import(cwdResolve(cwd + "/package.json"), {
        with: { type: "json" },
      })
    ).default;
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
      return false;
    }
  }
  // #regionend

  const npm_cmd = new Deno.Command(cmdWhich, {
    cwd,
    args: ["publish", "--access", "public"],
    stdout: "inherit",
    stderr: "inherit",
    stdin: "inherit",
  });
  const process = npm_cmd.spawn();
  const status = await process.status;
  return status.success;
};

/**
 * 将 package.json 的 version 同步到 npm.*.json 中
 */
export const doPubFromJson = async (
  inputConfigFile: string,
  target: string,
  outputConfigFile = /^http(s{0,1})\:\/\//.test(inputConfigFile) ? undefined : inputConfigFile,
  autoCheck: boolean
) => {
  const npmConfigs = (await import(inputConfigFile, { with: { type: "json" } })).default;
  const config = npmConfigs[target];
  if (await doPubCore(config.buildToRootDir, autoCheck)) {
    /// 更新配置文件
    config.version = (
      await import(cwdResolve(config.buildToRootDir + "/package.json"), {
        with: { type: "json" },
      })
    ).default.version;
  }
  /// 写入配置文件
  if (outputConfigFile) {
    Deno.writeFileSync(
      new URL(outputConfigFile, import.meta.url),
      new TextEncoder().encode(JSON.stringify(npmConfigs, null, 2)),
      {}
    );
  }
};

export const doPub = async (args = Deno.args) => {
  const targets =
    args.length === 0 ? ["plugins", "cli", "server", "is-dweb", "core", "helper", "js-process", "polyfill"] : args;
  for (const target of targets) {
    console.log(picocolors.blue(`start pub ${picocolors.cyan(target)}:`));
    await doPubFromJson(import.meta.resolve(`./publish.json`), target, undefined, true);
  }
};

if (import.meta.main) {
  doPub();
}
