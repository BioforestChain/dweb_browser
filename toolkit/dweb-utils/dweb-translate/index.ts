import node_path from "node:path";
import translate from "npm:translate";
import { readRelativeFile } from "./helpers/file.ts";
import { startCli } from "./src/cli.ts";

/**入口函数 */
const main = () => {
  const parsed = startCli();
  const files = [...parsed.args];
  const fromLang = parsed.options.from;
  const toLang = parsed.options.to;
  const outDir = parsed.options.outDir || "i18ndir";
  //  必须都传递才开始识别
  if (fromLang && toLang) {
    translateFlow(files, fromLang, toLang, outDir);
  }
  // 没有传递参数，尝试读取配置文件
};

/**开始翻译 */
const translateFlow = (sourceFiles: string[], fromLang: string, toLang: string, outDir: string) => {
  sourceFiles.forEach(async (sourcePath) => {
    const content = readRelativeFile(sourcePath);
    const result = await translate(content, { from: fromLang, to: toLang });
    outputFileFlow(sourcePath, result, outDir, toLang);
  });
};

/**输出翻译文件 */
const outputFileFlow = (sourcePath: string, data: string, outDir: string, toLang: string) => {
  // 从源文件路径中提取文件名
  const fileName = sourcePath.split("/").pop();
  if (!fileName) {
    throw new Error("Failed to extract the file name!");
  }
  // 匹配文件名和扩展名的正则表达式
  const regex = /^(.*)\.(.*)$/;
  const targetName = sourcePath.replace(regex, `$1.${toLang}.$2`);

  const targetDir = node_path.join(Deno.cwd(), outDir);

  // 看看需不需要创建打包目录
  try {
    Deno.statSync(targetDir).isDirectory;
  } catch {
    Deno.mkdirSync(outDir, { recursive: true });
  }
  // 创建输出文件的完整路径
  const outputFile = node_path.join(targetDir, targetName);
  // 将翻译内容写入文件
  try {
    Deno.writeTextFileSync(outputFile, data);
    console.log(`✅ The translation file has been saved to: ${outputFile}`);
  } catch (error) {
    console.error(`💢 There was an error writing to the file: ${outputFile}`, error);
  }
};

main();
