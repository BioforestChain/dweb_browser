import { fs, logColors, path } from "../../deps.ts";
import { $BFSMetaData } from "../types/metadata.type.ts";

export async function createFile(fileName: string, obj: $BFSMetaData) {
  const file = await Deno.open(fileName, {
    create: true,
    write: true,
    truncate: true,
  });
  await file.write(new TextEncoder().encode(JSON.stringify(obj, null, 2)));
  file.close();
}

/**
 * 创建打包目录
 * @param bfsAppId 应用appid，必须在当前的https域名下
 *  * @returns {boolean}
 */
export async function createBfsaDir(
  destPath: string,
  bfsAppId: string
): Promise<string> {
  try {
    const temporaryPath = path.join(destPath, bfsAppId);
    await fs.emptyDir(temporaryPath);

    const mkdir = Deno.mkdir;
    // 创建bfsApp目录
    await mkdir(temporaryPath, { recursive: true });
    await mkdir(path.join(temporaryPath, "boot"));
    await mkdir(path.join(temporaryPath, "sys"));
    await mkdir(path.join(temporaryPath, "tmp"));
    await mkdir(path.join(temporaryPath, "home"));
    await mkdir(path.join(temporaryPath, "usr/bfs_worker"),{
      recursive: true,
    });

    return temporaryPath;
  } catch (ex) {
    throw Error(ex.message);
  }
}

/**
 * 搜索文件获取地址
 * @param src     搜索文件路径
 * @param nameReg 搜索文件正则
 * @returns
 */
export async function searchFile(
  src: string,
  nameReg: RegExp
): Promise<string> {
  let searchPath = "";
  await catchFunctionType(loopSearchFile, src, nameReg);
  async function loopSearchFile(src: string, nameReg: RegExp) {
    const entries = Deno.readDir(src);
    for await (const entry of entries) {
      const filePath = path.join(src, entry.name!);
      if (nameReg.test(entry.name!) && entry.isFile) {
        searchPath = filePath;
        return searchPath;
      } else if (entry.isDirectory && entry.name !== "node_modules") {
        await loopSearchFile(filePath, nameReg);
      }
    }
  }

  return searchPath;
}

/**
 * 复制目录
 * @param src  源目录
 * @param dest 目标目录
 */
export async function copyDir(src: string, dest: string) {
  await fs.copy(src, dest, { overwrite: true });
}

/**
 * 捕获文件不存在的错误
 * @param fun
 * @param args
 * @returns
 */
export const catchFunctionType = async <R>(
  // deno-lint-ignore no-explicit-any
  fun: (...args: any) => R,
  // deno-lint-ignore no-explicit-any
  ...args: any
) => {
  try {
    return await fun(...args);
  } catch (error) {
    if (error instanceof Deno.errors.NotFound) {
      throw logColors.bgRed(`The passed folder does not exist, please check the path：${error.message}`);
    }
    throw error;
  }
};

const forwardSlashRegEx = /\//g;
const CHAR_LOWERCASE_A = 97;
const CHAR_LOWERCASE_Z = 122;
/**
 * file/// 路径转化为
 * @param filePath
 */
export const filePathToUrl = (path: string | URL) => {
  const isWindows = Deno.build.os === "windows";
  let url = path;
  if (typeof path === "string") {
    url = new URL(path);
  } else if (!(url instanceof URL)) {
    throw new Deno.errors.InvalidData(
      "invalid argument path , must be a string or URL"
    );
  }
  if (url.protocol !== "file:") {
    throw new Deno.errors.InvalidData("invalid url scheme");
  }
  return isWindows ? getPathFromURLWin(url) : getPathFromURLPosix(url);
};

function getPathFromURLWin(url: URL) {
  const hostname = url.hostname;
  let pathname = url.pathname;
  for (let n = 0; n < pathname.length; n++) {
    if (pathname[n] === "%") {
      const third = pathname.codePointAt(n + 2) || 32;
      if (
        (pathname[n + 1] === "2" && third === 102) ||
        (pathname[n + 1] === "5" && third === 99)
      ) {
        throw new Deno.errors.InvalidData(
          "must not include encoded \\ or / characters"
        );
      }
    }
  }
  pathname = pathname.replace(forwardSlashRegEx, "\\");
  pathname = decodeURIComponent(pathname);
  if (hostname !== "") {
    return `\\\\${hostname}${pathname}`;
  } else {
    const letter = pathname.codePointAt(1)! | 32;
    const sep4 = pathname[2];
    if (
      letter < CHAR_LOWERCASE_A ||
      letter > CHAR_LOWERCASE_Z ||
      sep4 !== ":"
    ) {
      throw new Deno.errors.InvalidData("file url path must be absolute");
    }
    return pathname.slice(1);
  }
}
function getPathFromURLPosix(url: URL) {
  if (url.hostname !== "") {
    throw new Deno.errors.InvalidData("invalid file url hostname");
  }
  const pathname = url.pathname;
  for (let n = 0; n < pathname.length; n++) {
    if (pathname[n] === "%") {
      const third = pathname.codePointAt(n + 2) || 32;
      if (pathname[n + 1] === "2" && third === 102) {
        throw new Deno.errors.InvalidData(
          "must not include encoded / characters"
        );
      }
    }
  }
  return decodeURIComponent(pathname);
}
