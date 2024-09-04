import { join } from "@std/path";
import translate from "npm:translate";
import { readRelativeFile } from "../helpers/file.ts";
import type { $ConfigMetadata } from "./config.ts";
/**开始翻译 */
export const translateFlow = (config: $ConfigMetadata) => {
  const { files, from, to, outDir } = config;
  files.forEach(async (sourcePath) => {
    const content = readRelativeFile(sourcePath);
    const result = await translate(content, { from: from, to: to });
    outputFileFlow(sourcePath, result, outDir, to);
  });
};

/**输出翻译文件 */
export const outputFileFlow = (sourcePath: string, data: string, outDir: string, toLang: string) => {
  // 从源文件路径中提取文件名
  const fileName = sourcePath.split("/").pop();
  if (!fileName) {
    throw new Error("Failed to extract the file name!");
  }
  // 匹配文件名和扩展名的正则表达式
  const regex = /^(.*)\.(.*)$/;
  const targetName = sourcePath.replace(regex, `$1.${toLang}.$2`);

  const targetDir = new URL(`${Deno.cwd()}/${outDir}`, `file://`).pathname;

  // 看看需不需要创建打包目录
  try {
    Deno.statSync(targetDir).isDirectory;
  } catch {
    Deno.mkdirSync(outDir, { recursive: true });
  }
  // 创建输出文件的完整路径
  const outputFile = join(targetDir, targetName);
  // 将翻译内容写入文件
  try {
    Deno.writeTextFileSync(outputFile, data);
    console.log(`✅ The translation file has been saved to: ${outputFile}`);
  } catch (error) {
    console.error(`💢 There was an error writing to the file: ${outputFile}`, error);
  }
};
