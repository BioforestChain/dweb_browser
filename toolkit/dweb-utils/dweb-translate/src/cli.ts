import { cac } from "https://unpkg.com/cac@6.7.14/mod.ts";
import { findPackageJson } from "../../helpers/index.ts";

/**启动cli */
export const startCli = (): ParsedArgv => {
  const cli = cac("dwebt");
  const packageJson = findPackageJson("dweb-translate");
  cli
    .command("[...files]", "Translate files")
    .option("-f,--from <lang>", "Source file language.")
    .option("-t,--to <lang>", "Translate the output language.")
    .option("-o,--outDir [dir]", "The directory where the output is translated.")
    .option("-c,--config <file>", "Specify the profile for the translation.");
  // .option("-m, --mode", "Translation mode.");

  cli.help();
  cli.version(`v${packageJson.version}`);

  return cli.parse();
};

interface ParsedArgv {
  args: ReadonlyArray<string>;
  options: {
    // deno-lint-ignore no-explicit-any
    [k: string]: any;
  };
}
