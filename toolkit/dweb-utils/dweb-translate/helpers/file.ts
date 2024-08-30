/**
 * 读取相对于命令行位置的文件内容
 * @param path string
 */
export const readRelativeFile = (path: string) => {
  // 获取当前工作目录的路径
  const fileURL = new URL(`file://${Deno.cwd()}/${path}`);
  return Deno.readTextFileSync(fileURL);
};
