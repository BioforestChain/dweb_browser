import { Tar, Untar, copy, fs, path } from "../../deps.ts";

/**
 * 压缩目录为bfsa后缀
 * @param dest     压缩文件目录
 * @param bfsAppId 应用id
 */
export async function compressToSuffixesBfsa(dest: string, bfsAppId: string) {
  const tar = new Tar();
  for await (const entry of fs.walk(dest)) {
    if (!entry.isFile) {
      continue;
    }
    let filePath = path.join(bfsAppId, entry.path.slice(dest.length));
    if (Deno.build.os === "windows") {
      filePath = filePath.replace(/\\/g, "/");
    }
    tar.append(filePath, {
      filePath: entry.path,
    });
  }
  // use tar.getReader() to read the contents.
  const bfsaPath = path.resolve(dest, "../", `${bfsAppId}.jmm`);
  const writer = await Deno.open(bfsaPath, {
    write: true,
    create: true,
  });
  await copy(tar.getReader(), writer);
  writer.close();
  return bfsaPath;
}

/**
 * 解压
 * @param file 压缩包名
 * @param dest 目标地址
 */
export async function uncompressBfsa(file: string) {
  const reader = await Deno.open(file, { read: true });
  const untar = new Untar(reader);

  for await (const entry of untar) {
    if (entry.type === "directory") {
      await fs.ensureDir(entry.fileName);
      continue;
    }

    await fs.ensureFile(entry.fileName);
    const file = await Deno.open(entry.fileName, { write: true });
    // <entry> is a reader.
    await copy(entry, file);
  }
  reader.close();
}
