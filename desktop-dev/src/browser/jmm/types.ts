import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { $CommonAppManifest, $MMID } from "../../core/types.ts";

/** Js模块应用 元数据 */
export interface $JmmAppManifest extends Required<$CommonAppManifest> {
  id: $MMID;
  /** 版本信息 */
  version: string;
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
  icon: string;
  /** 安装时展示用的截图 */
  images: string[];
  bundle_url: string;
  bundle_hash: string;
  bundle_size: number;
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
