import { cac } from "https://unpkg.com/cac@6.7.14/mod.ts";
import { findPackageJson } from "../../helpers/index.ts";
import { langs } from "../helpers/ISO_639-1.ts";

/**启动cli */
export const startCli = (): ParsedArgv => {
  const cli = cac("dwebt");
  const packageJson = findPackageJson("dweb-translate");
  cli
    .command("[...files]", "Translate files")
    .option("-f,--from <lang>", "Source file language.")
    .option("-t,--to <lang>", "Translate the output language.")
    .option("-o,--outDir [dir]", "The directory where the output is translated.")
    .option("-c,--config <file>", "Specify the profile for the translation.")
    .action((_files, options) => {
      const from: string | undefined = options.from;
      const to: string | undefined = options.to;
      const config: string | undefined = options.config;
      // 如果传递了配置文件，则忽略指令验证
      if (config) {
        return;
      }
      // 如果有一者不存在，则默认读取配置文件
      if (!from || !to) {
        return;
      }
      // 验证语言类型
      // 预先计算支持语言的集合和错误消息，减少重复计算
      const supportedLanguages = new Set(Object.keys(langs));
      const supportedLanguagesList = Array.from(supportedLanguages).join(",");
      // 验证语言类型
      const validateLanguage = (lang: string, langType: "from" | "to") => {
        if (!supportedLanguages.has(lang)) {
          throw new Error(`Invalid ${langType} language: ${lang}. Supported languages are ${supportedLanguagesList}.`);
        }
      };
      // 验证来源语言和目标语言
      validateLanguage(from, "from");
      validateLanguage(to, "to");
      //验证结束
    });
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
