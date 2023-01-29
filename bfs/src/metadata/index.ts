import { IDwebview, IImportMap, IManifest, IMetaData } from "./metadataType.ts";
export { LinkMetadata, Files } from "./link.ts";
export class MetaData implements IMetaData {
  manifest: Manifest;
  dwebview: DWebView;
  whitelist?: string[];
  constructor(metaData: IMetaData) {
    this.manifest = metaData.manifest;
    this.dwebview = metaData.dwebview;
    this.whitelist = metaData.whitelist;
  }
}

export class Manifest implements IManifest {
  version!: string;
  name!: string;
  icon!: string;
  author!: string[];
  description!: string;
  keywords!: string[];
  privateKey!: string;
  homepage!: string;
  // 应用最大缓存时间
  maxAge!: number;
  enters!: string[];
  //本次发布的信息，一般存放更新信息
  releaseNotes!: string;
  //  本次发布的标题，用于展示更新信息时的标题
  releaseName!: string;
  // 发布日期
  releaseDate!: string;
  // constructor(meta: IManifest) {
  //   this.origin = meta.origin;
  //   this.author = meta.author;
  //   this.description = meta.description;
  //   this.keywords = meta.keywords;
  //   this.dwebId = meta.dwebId;
  //   this.privateKey = meta.privateKey;
  //   this.enter = meta.enter;
  // }
}

export class DWebView implements IDwebview {
  importmap!: ImportMap[];
}

export class ImportMap implements IImportMap {
  url!: string;
  response!: string;
}

export function metaConfig(metaData: MetaData) {
  return new MetaData(metaData)
}
