export interface ShareOptions {
  /**
   * 为任何消息设置标题
   *
   * @since 1.0.0
   */
  title?: string;

  /**
   * 设置一些文字分享
   *
   * @since 1.0.0
   */
  text?: string;

  /**
   * 设置要分享的 URL，可以是 http、https 或 file:// URL
   *
   * @since 1.0.0
   */
  url?: string;

  /**
   * 要共享的文件 单文件
   */
  file?: File;

  /**
   * 要共享的文件的 file:// URL 数组。
   * 仅支持 iOS 和 Android。
   * @since 4.1.0
   */
  files?: FileList;

  /**
   * 用于图片压缩，使用canvas进行压缩
   */
  imageBlobOptions?: ImageBlobOptions
}

export interface ShareResult {
  success: boolean;
  message: string;
}

export interface FileData {
  name: string;
  type: string;
  size: number;
  encode: FileDataEncode;
  data: string;
}

export enum FileDataEncode {
  UTF8 = "utf8",
  BASE64 = "base64"
}

export interface ImageBlobOptions {
  type: string;
  quality: number;
}
