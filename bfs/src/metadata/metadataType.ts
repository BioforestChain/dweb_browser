export interface IMetaData {
  manifest: IManifest;
  dwebview: IDwebview;
  whitelist?: string[];
}
export interface IManifest {
  // app 更新的版本
  version: string;
  // app 名称
  name: string;
  // app 图标
  icon: string;
  // 开发者
  author: string[];
  // 应用搜索的描述
  description: string;
  // 应用最大缓存时间
  maxAge: number;
  // 应用搜索的关键字
  keywords: string[];
  // 应用介绍主页
  homepage: string;
  // 私钥文件，用于最终的应用签名
  privateKey: string;
  // 应用入口，可以配置多个，其中index为缺省名称。
  // 外部可以使用 DWEB_ID.bfchain (等价同于index.DWEB_ID.bfchain)、admin.DWEB_ID.bfchain 来启动其它页面
  enters: string[];
  //本次发布的信息，一般存放更新信息
  releaseNotes: string;
  //  本次发布的标题，用于展示更新信息时的标题
  releaseName: string;
  // 发布日期
  releaseDate: string;
}

export interface IDwebview {
  importmap: IImportMap[];
}

export interface IImportMap {
  url: string;
  response: string;
}
