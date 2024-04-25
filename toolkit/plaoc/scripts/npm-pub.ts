import { whichSync } from "../../scripts/helper/WhichCommand.ts";
import { colors } from "../cli/deps.ts";
import { cwdResolve } from "./cwd.ts";

export const doPubCore = async (cwd: string, autoCheck = false) => {
  const cmdWhich = whichSync("npm")!;
  if (autoCheck) {
    const packageJson = (
      await import(cwdResolve(cwd + "/package.json"), {
        assert: { type: "json" },
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
        colors.yellow(`package ${colors.cyan(packageJson.name)}${colors.dim('@')}${colors.green(packageJson.version)} already exists`)
      );
      return false;
    }
  }
  const npm_cmd = new Deno.Command(cmdWhich, {
    cwd,
    args: ["publish", "--access", "public"],
    stdout: "inherit",
    stderr: "inherit",
    stdin: "inherit",
  });
  const process = await npm_cmd.spawn();
  const status = await process.status;
  return status.success;
};

/**
 * 将 package.json 的 version 同步到 npm.*.json 中
 */
export const doPubFromJson = async (
  inputConfigFile: string,
  outputConfigFile = /^http(s{0,1})\:\/\//.test(inputConfigFile) ? undefined : inputConfigFile,
  autoCheck: boolean
) => {
  const npmConfigs = (await import(inputConfigFile, { assert: { type: "json" } })).default;

  for (const config of npmConfigs) {
    if (await doPubCore(config.buildToRootDir, autoCheck)) {
      /// 更新配置文件
      config.version = (
        await import(cwdResolve(config.buildToRootDir + "/package.json"), {
          assert: { type: "json" },
        })
      ).default.version;
    }
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
  /**
   * 如果不传入任何参数，那么就自动检测版本
   */
  const autoCheck = args.length === 0;
  const targets = args.length === 0 ? ["client", "cli", "server", "is-dweb"] : args;
  for (const target of targets) {
    console.log(colors.blue(`start pub ${colors.cyan(target)}:`))
    await doPubFromJson(import.meta.resolve(`./npm.${target}.json`), undefined, autoCheck);
  }
};

if (import.meta.main) {
  doPub();
}
