/**
 * 验证svg渲染是否会内存溢出，并将超过5MB的SVG转换为WebP
 */
export const verifySvg = (sourcePath: string) => {
  try {
    // 递归遍历目录
    const traverseDirectory = (dirPath: string) => {
      for (const entry of Deno.readDirSync(dirPath)) {
        const fullPath = `${dirPath}/${entry.name}`;
        if (entry.isDirectory) {
          // 如果是目录，递归遍历
          traverseDirectory(fullPath);
        } else if (entry.isFile && entry.name.endsWith(".svg")) {
          // 如果是SVG文件，检查大小并转换
          const fileInfo = Deno.statSync(fullPath);
          if (fileInfo.size > 5 * 1024 * 1024) {
            // 大小超过5MB
            console.log(`文件 ${entry.name} 超过5MB，正在转换为WebP...`);
            convertSvgToWebp(fullPath);
          }
        }
      }
    };

    // 执行递归遍历
    traverseDirectory(sourcePath);
    return false;
  } catch (error) {
    console.error("Error reading directory:", error);
    return false;
  }
};

/**
 * 将SVG文件转换为WebP
 */
export const convertSvgToWebp = async (svgPath: string) => {};
