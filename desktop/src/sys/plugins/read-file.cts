import fsPromises from "node:fs/promises"
import path from "node:path"
// 读取 html 文件
export async function reqadHtmlFile(filename: string){
    const targetPath = path.resolve(
      process.cwd(),
      `./assets/html/${filename}.html`
    );
    const content = await fsPromises.readFile(targetPath)
    return new TextDecoder().decode(content)
}