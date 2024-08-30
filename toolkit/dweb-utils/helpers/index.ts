/**查找package.json内容 */
export const findPackageJson = (name: string): { version: string } => {
  try {
    const newCur = new URL(`../${name}/package.json`, import.meta.url);
    const content = Deno.readTextFileSync(newCur);
    return JSON.parse(content);
  } catch {
    return { version: "0.0.0-dev" };
  }
};
