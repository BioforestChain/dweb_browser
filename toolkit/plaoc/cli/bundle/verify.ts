import path from "node:path";
import { fileURLToPath } from "node:url";
import { detect_svg_render, svg_to_webp } from "npm:@dweb-browser/svg-wasm";
import { colors } from "../deps/cliffy.ts";
import "../platform/initWasm.deno.ts";

/**
 * 验证svg渲染是否会内存溢出，并将超过5MB的SVG转换为WebP
 */
export const verifySvg = async (sourcePath: string) => {
  let first = true; // 标识告警
  try {
    // 递归遍历目录
    const traverseDirectory = async (dirPath: string) => {
      for (const entry of Deno.readDirSync(dirPath)) {
        const fullPath = path.resolve(dirPath, entry.name);
        if (entry.isDirectory) {
          // 如果是目录，递归遍历
          traverseDirectory(fullPath);
        } else if (entry.isFile && entry.name.endsWith(".svg")) {
          const svg_buffer = await Deno.readFile(fullPath);
          // 如果不能渲染会造成内存溢出
          if (!detect_svg_render(svg_buffer)) {
            first &&
              console.log(
                colors.yellow(`⚠️ 检测到会导致内存溢出的图片，已经帮助转化为webp，请替换以下资源,之后重新打包！`)
              );
            console.log(colors.yellow(`⚠️ ${fullPath}`));
            first = false;
            try {
              await convertSvgToWebp(fullPath, svg_buffer);
            } catch {
              console.log(colors.yellow(`⚠️请在代码替换资源之后重新打包！`));
            }
          }
        }
      }
    };

    // 执行递归遍历
    await traverseDirectory(path.resolve(Deno.cwd(), sourcePath));
  } catch (error) {
    console.error("Error reading directory:", error);
    return false;
  }
  return first;
};

/**
 * 将SVG文件转换为WebP 让用户替换原文件
 */
export const convertSvgToWebp = async (svg_path: string, svg_buffer: Uint8Array) => {
  const webpPath = svg_path.replace(".svg", ".webp");
  const webpBuffer = svg_to_webp(svg_buffer);
  await Deno.writeFile(webpPath, webpBuffer);
  return webpPath;
};
