export const doPub = async (outDir:string) => {
  const command = new Deno.Command("npm", {
    cwd:outDir,
    args:[
      "publish",
      "--access=public",
    ],
  });
  const { stderr,stdout } = await command.output();
  console.error(new TextDecoder().decode(stdout));
  console.error(new TextDecoder().decode(stderr));
};

export const doPubFromJson = async (inputConfigFile: string) => {
  const npmConfigs = (
    await import(inputConfigFile, { assert: { type: "json" } })
  ).default;

  await doPub(npmConfigs.buildToRootDir);
};

if (import.meta.main) {
  await doPubFromJson(import.meta.resolve("./npm.json"));
}
