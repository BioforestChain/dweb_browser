// 获取全部的 app信息
import fsPromises from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { $AppMetaData } from "../jmm/jmm.ts";

export async function getAllApps() {
  const appsPath = path.resolve(process.cwd(), "./apps/infos");
  const foldersName: string[] = await fsPromises.readdir(appsPath);
  const appsInfo: $AppMetaData[] = [];
  foldersName.forEach(async (folderName: string) => {
    const metaData = (await JSON.parse(
      await fsPromises.readFile(
        path.resolve(appsPath, `./${folderName}/package.json`),
        "utf-8"
      )
    )) as $AppMetaData;
    appsInfo.push(metaData);
  });
  return appsInfo;
}

export interface $StaticWebServers {
  root: string;
  entry: string;
  subdomain: string;
  port: number;
}

/**
 * 第三方应用的 app信息
 */
export interface $AppInfo {
  folderName: string; // 目录
  appId: string; // 全部小写后的 bfsAppId
  version: string; // 版本信息
  bfsAppId: string;
  name: string;
  icon: string;
}
