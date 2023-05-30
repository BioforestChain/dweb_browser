import { Input, logColors, path } from "../../deps.ts";
import {
  $BFSMetaData,
  $UserMetadata,
  expectedKeys,
} from "../types/metadata.type.ts";
import type { IProblemConfig } from "../types/problem.type.ts";
import { compressToSuffixesBfsa } from "../utils/compress.ts";
import {
  catchFunctionType,
  copyDir,
  createBfsaDir,
  createFile,
  filePathToUrl,
  searchFile,
} from "../utils/file.ts";

/**
 * 打包入口
 * @param options
 */
export async function bundle(options: IProblemConfig) {
  let { destPath, frontBuildPath } = options;
  // 判断用户输入的是绝对地址还是相对地址
  destPath = path.isAbsolute(destPath)
    ? destPath
    : path.resolve(Deno.cwd(), destPath);
  // 在用户输入的根目录先拿到metadata.json
  const metadata = await createBfsaMetaData(destPath);

  const bfsAppId = metadata.id;

  const temporaryPath = await createBfsaDir(destPath, bfsAppId);

  // 将前端项目移动到sys目录 (无界面应用不包含前端)
  const usrPath = path.join(temporaryPath, "usr");
  if (frontBuildPath) {
    // 如果是纯后端应用则不需要复制
    await copyDir(frontBuildPath, usrPath);
  }

  // 将后端项目编译到sys目录
  const workerPath = path.join(
    temporaryPath,
    "usr/bfs_worker/public.service.worker.js"
  );
  const workerUrl = new URL("./public.service.worker.js", import.meta.url);
  const workerJs = filePathToUrl(workerUrl.href);
  await Deno.copyFile(workerJs, workerPath);

  //TODO 配置文件写入boot目录
  // const bootPath = path.join(destPath, "boot");
  // await writeConfigJson(bootPath, metadata);

  // 对文件进行压缩
  const appPath = await compressToSuffixesBfsa(temporaryPath, bfsAppId);

  // 压缩完成，删除目录
  await Deno.remove(temporaryPath, { recursive: true });

  const appStatus = await Deno.stat(appPath);
  // 添加一些需要编译完才能拿到的属性
  metadata.size = appStatus.size;
  metadata.releaseDate = appStatus.mtime;

  // 生成bfs-metadata.json
  const bfsMetaPath = path.resolve(destPath, "bfs-metadata.json");
  createFile(bfsMetaPath, metadata);

  console.log(logColors.bgRgb24("bundle jmm application done!!!",0x008080));
}

/**
 * 获取bfsa-metadata.json文件的数据
 * @param bootPath boot目录
 * @returns
 */
async function createBfsaMetaData(destPath: string) {
  const bfsMetaPath = await searchMetadata(destPath);

  const bfsMetaU8 = await Deno.readTextFile(bfsMetaPath);
  const bfsMeta: $UserMetadata = JSON.parse(bfsMetaU8);
  const bfsUrl = new URL(bfsMeta.home);

  const actualKeys = Object.keys(bfsMeta);
  let missingKeys = "";
  if (
    !expectedKeys.every((key) => {
      const missing = actualKeys.includes(key);
      if (!missing) {
        missingKeys = `${missingKeys} ${key}`;
      }
      return missing;
    })
  ) {
    throw new Error(logColors.red(`Not added ${missingKeys} to the bfs-jmm.json file`),{
      
    });
  }
  const _metadata: $BFSMetaData = {
    id: `${bfsMeta.name}.${bfsUrl.host}.dweb`,
    server: {
      root: "/usr",
      entry: "/bfs_worker/public.service.worker.js", // 后端未开放先固定
    },
    title: bfsMeta.name,
    subtitle: bfsMeta.subName,
    icon: bfsMeta.icon,
    downloadUrl: bfsMeta.downloadUrl,
    images: bfsMeta.images,
    introduction: bfsMeta.introduction,
    author: bfsMeta.author,
    version: bfsMeta.version,
    newFeature: bfsMeta.newFeature,
    keywords: bfsMeta.keywords ?? [],
    home: bfsMeta.home,
    size: 0,
    fileHash: "",
    plugins: [],
    releaseDate: null,
  };
  return _metadata;
}

/**
 * 适配用户传递bfs-jmm.json的情况
 * @param metaPath bfs-jmm
 * @returns
 */
async function searchMetadata(destPath: string) {
  // console.log("Project address :", colors.green(destPath));
  // 搜索bfs-link.ts
  const bfsMetaPath = await searchFile(destPath, /^bfs-jmm\.json$/i);
  if (bfsMetaPath === "") {
    const bfsPath = await Input.prompt(
      logColors.bgBlue("The address of the configuration file was not found, please enter the address of the bfs-jmm.json configuration file:")
    );
    await catchFunctionType(Deno.stat, bfsPath);
    const fileInfo = await Deno.stat(bfsPath);
    if(fileInfo.isDirectory) {
      const bfsMetaPath = await searchFile(destPath, /^bfs-jmm\.json$/i);
      if(bfsMetaPath === "") {
        throw logColors.bgRed(`The configuration file address was not found!`);
      }
    }
    return bfsPath;
  }
  return bfsMetaPath;
}

// /**
//  * 为文件列表生成sha512校验码
//  * @param dest        查找目录
//  * @param bfsAppId    应用id
//  * @param filesList   文件列表hash
//  * @returns
//  */
// async function fileListHash(
//   dest: string,
//   bfsAppId: string,
//   filesList: Files[]
// ): Promise<Files[]> {
//   const entries =  readDir(dest);

//   for await (const entry of entries) {
//     const filePath = path.join(dest, entry.name!);

//     if (entry.isFile) {
//       const fileStat = await stat(filePath);
//       const fileHash = await checksumFile(filePath, "sha512", "hex");
//       const file = {
//         url: `https://shop.plaoc.com/${bfsAppId}${slash(
//           filePath.slice(filePath.lastIndexOf(bfsAppId) + bfsAppId.length)
//         )}`,
//         size: fileStat.size,
//         sha512: fileHash,
//       };

//       filesList.push(file);
//     } else if (entry.isDirectory) {
//       await fileListHash(filePath, bfsAppId, filesList);
//     }
//   }

//   return filesList;
// }
