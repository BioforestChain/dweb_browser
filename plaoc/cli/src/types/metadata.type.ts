import { $MMID } from "./problem.type.ts";

export interface $IAppversion {
  data: {
    version: string;
    icon: string;
    name: string;
    files: [];
    releaseNotes: string;
    releaseName: string;
    releaseDate: string;
  };
  errorCode: number;
  errorMsg: string;
}

// 用户需要填写的内容，即bfs-jmm.json必填的内容
export interface $UserMetadata {
  name: string;
  subName: string;
  version: string; // 应用版本
  introduction: string; // 应用描述
  author: string[]; // 开发者，作者
  icon: string; // 应用图标
  images: string[]; // 应用截图
  newFeature: string; // 新功能
  home: string; // 首页地址
  downloadUrl: string; // 下载应用地址
  keywords?: string[]; // 关键词
}

export const expectedKeys = [
  "name",
  "subName",
  "version",
  "introduction",
  "author",
  "icon",
  "images",
  "newFeature",
  "home",
  "downloadUrl",
];

export interface $BFSMetaData {
  id: $MMID;
  server: $MainServer; // 打开应用地址
  title: string; // 应用名称
  subtitle: string; // 应用副标题
  icon: string; // 应用图标
  downloadUrl: string; // 下载应用地址
  images: string[]; // 应用截图
  introduction: string; // 应用描述
  author: string[]; // 开发者，作者
  version: string; // 应用版本
  newFeature: string; // 新功能
  keywords: string[]; // 关键词
  home: string; // 首页地址
  size: number; // 应用大小
  fileHash: string;
  plugins: string[];
  releaseDate: Date | null; // 发布时间
}

interface $MainServer {
  /**
   * 应用文件夹的目录
   */
  root: string;
  /**
   * 入口文件
   */
  entry: string;
}
