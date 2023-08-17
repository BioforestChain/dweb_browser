import { cwdResolve } from "./cwd.ts";

export const doPubCore = async (cwd: string) => {
  const npm_cmd = new Deno.Command("npm", {
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

export const doPubFromJson = async (
  inputConfigFile: string,
  outputConfigFile = /^http(s{0,1})\:\/\//.test(inputConfigFile) ? undefined : inputConfigFile
) => {
  const npmConfigs = (await import(inputConfigFile, { assert: { type: "json" } })).default;

  for (const config of npmConfigs) {
    if (await doPubCore(config.buildToRootDir)) {
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
  const target = args[0] ?? "client";
  await doPubFromJson(import.meta.resolve(`./npm.${target}.json`));
};

if (import.meta.main) {
  doPub();
}
