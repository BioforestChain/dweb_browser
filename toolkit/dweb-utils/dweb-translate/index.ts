import { startCli } from "./src/cli.ts";
import { readConfig } from "./src/config.ts";
import { translateFlow } from "./src/flow.ts";

/**入口函数 */
const main = () => {
  // 解析指令
  const parsed = startCli();
  // 传递的需要翻译的文件
  const files = [...parsed.args];
  const from: string = parsed.options.from;
  const to: string = parsed.options.to;
  const outDir = parsed.options.outDir || "i18ndir";
  const configDir: string | undefined = parsed.options.config;

  //  必须都传递才开始识别
  if (from && to && files.length > 0) {
    return translateFlow({ files, from, to, outDir });
  }
  // 没有传递参数，尝试读取配置文件
  const config = readConfig(configDir);
  if (!config) {
    console.error("❌ You have to pass parameters or configuration files!");
    return;
  }
  return translateFlow(config);
};

main();
