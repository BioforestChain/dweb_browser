/// 这里是针对配置文件进行读取，传递给最终的翻译

export const readConfig = (configDir: string | undefined) => {
  // 如果没有指定配置文件则读取默认的配置文件
  if (!configDir) {
    configDir = "./dwebtranslaterc.json";
  }

  // 规范化路径，确保在不同操作系统上都能正确解析
  const configPath = new URL(`${Deno.cwd()}/${configDir}`, "file://").pathname;
  // 判断配置文件是否存在
  try {
    Deno.statSync(configPath);
  } catch {
    console.error(`Not Found the config file=> ${configPath}`);
    return;
  }
  const content = Deno.readTextFileSync(configPath);
  const json: $ConfigMetadata = JSON.parse(content);
  if (!validateConfig(json)) {
    throw new Error("Invalid config file: JSON is incomplete or malformed.");
  }
  return json;
};

/**验证配置文件参数是否完整，如果后续配置增加使用zod库验证 */
function validateConfig(json: $ConfigMetadata): json is $ConfigMetadata {
  // 检查所有必需的属性是否存在并且类型正确
  return (
    typeof json === "object" &&
    Array.isArray(json.files) &&
    json.files.every((file) => typeof file === "string") &&
    typeof json.to === "string" &&
    typeof json.from === "string" &&
    typeof json.outDir === "string"
  );
}

/**翻译配置文件
 * @files 翻译文件
 * @from 来源语言
 * @to 翻译目标语言
 * @outDir 输出目录
 */
export interface $ConfigMetadata {
  files: string[];
  to: string;
  from: string;
  outDir: string;
}
