export interface IProblemConfig {
  destPath:string; // 项目地址
  frontBuildPath: string; // 前端打包完的路径
  // backPath: string; // 后端路径 未开放
  // metaPath?:string; // metabata.json地址
}

export type $HTTPS = `https://${string}`;
export type $MMID = `${string}.${string}.dweb`;

export interface IManifest {
  host: string, // 域名地址，要求https
}