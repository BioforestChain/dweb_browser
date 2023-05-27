// 获取全部的 app信息
import fsPromises from "node:fs/promises";
import path from "node:path";
import { JMM_APPS_PATH } from "../jmm/jmm.api.serve.ts";
import { $AppMetaData } from "../jmm/jmm.ts";

export async function getAllApps() {
  const appsInfo: $AppMetaData[] = [];
  for (const app_id of await fsPromises.readdir(JMM_APPS_PATH)) {
    const metaData = (await JSON.parse(
      await fsPromises.readFile(
        path.join(JMM_APPS_PATH, app_id, `usr/metadata.json`),
        "utf-8"
      )
    )) as $AppMetaData;
    appsInfo.push(metaData);
  }
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
