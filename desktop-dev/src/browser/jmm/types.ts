import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { $CommonAppManifest, $DWEB_DEEPLINK, $MMID } from "../../core/types.ts";

/** Js模块应用 元数据 */
export interface $JmmAppManifest extends Required<$CommonAppManifest> {
  id: $MMID;
  /** 基准URL，如果没有定义了这个url，那么默认使用当前的 .json 链接 */
  baseURI?: string;
  /** 版本信息 */
  version: string;
  /**  */
  dweb_deeplinks?: $DWEB_DEEPLINK[];
  /** 类目 */
  categories: MICRO_MODULE_CATEGORY[];
  /** js 应用程序的入口 */
  server: {
    /** root 定义程序的启动目录 */
    root: string;
    /** root 定义程序的启动文件 */
    entry: string;
  };
}

/** Js模块应用安装使用的元数据 */
export interface $JmmAppInstallManifest extends $JmmAppManifest {
  /** 安装是展示用的 icon */
  logo: string;
  /** 安装时展示用的截图 */
  images: string[];
  bundle_url: string;
  bundle_hash: string;
  bundle_size: number;
  // app 支持的语言
  languages: string[]; // http://www.lingoes.net/zh/translator/langcode.htm
  /**格式为 `hex:{signature}` */
  bundle_signature: string;
  /**该链接必须使用和app-id同域名的网站链接，
   * 请求回来是一个“算法+公钥地址”的格式 "{algorithm}:hex;{publicKey}"，
   * 比如说 `rsa-sha256:hex;2...1` */
  public_key_url: string;
  /**更新日志，直接放url */
  change_log: string;
  /** 安装时展示的作者信息 */
  author: string[];
  /** 安装时展示的主页链接 */
  home: string;
  /** 安装时展示的发布日期 */
  release_date: string;
  /**
   * @deprecated 安装时显示的权限信息
   */
  permissions: string[];
  /**
   * @deprecated 安装时显示的依赖模块
   */
  plugins: string[];
}
