export class LinkMetadata {
  version!: string;
  bfsAppId!: string;
  name!: string;
  icon!: string;
  author!: string[];
  autoUpdate!: AutoUpdate;
}

export class AutoUpdate {
  maxAge!: number;
  provider!: number; // {"generic"} 该更新的适配器信息，默认使用“通用适配器”
  url!: string;
  version!: string;
  files!: Files[];
  releaseNotes!: string;
  releaseName!: string;
  releaseDate!: string;
}

export class Files {
  url!: string;
  size!: number;
  sha512!: string;
}
