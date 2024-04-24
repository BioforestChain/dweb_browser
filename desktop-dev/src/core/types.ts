export type $MMID = `${string}.dweb`;
export type $DWEB_DEEPLINK = `dweb:${string}`;
/**
 * 通讯支持的传输协议
 */
export interface $IpcSupportProtocols {
  cbor: boolean;
  protobuf: boolean;
  json: boolean;
}
import { MICRO_MODULE_CATEGORY } from "./helper/category.const.ts";
/**
 * 一种通用的 “应用” 元数据格式
 */
export interface $CommonAppManifest
  extends Pick<
    WebAppManifest,
    | "dir"
    | "lang"
    | "name"
    | "short_name"
    | "description"
    | "homepage_url"
    | "icons"
    | "screenshots"
    | "display"
    | "orientation"
    | "id"
    | "categories"
    | "theme_color"
    | "background_color"
    | "shortcuts"
  > {
  version?: string;
}
type RequiredByKey<T, K extends keyof T> = T & Required<Pick<T, K>>;

export interface $MicroModuleManifest extends RequiredByKey<Omit<$CommonAppManifest, "id">, "name"> {
  /** 模块id */
  readonly mmid: $MMID;
  /** 对通讯协议的支持情况 */
  readonly ipc_support_protocols: $IpcSupportProtocols;
  /**
   * 匹配的“DWEB深层链接”
   * 取代明确的 mmid，dweb-deeplinks 可以用来表征一种特性、一种共识，它必须是 'dweb://{domain}[/pathname[/pathname...]]' 的格式规范
   * 为了交付给用户清晰的可管理的模式，这里的 deeplink 仅仅允许精确的前缀匹配，因此我们通常会规范它的动作层级依次精确
   *
   * 比如说：'dweb://mailto'，那么在面对 'dweb://mailto?address=someone@mail.com&title=xxx' 的链接时，该链接会被打包成一个 IpcRequest 消息传输过来
   * 比如说：'dweb://open/file/image'，那么就会匹配这样的链接 'dweb://open/file/image/svg?uri=file:///example.svg'
   *
   * dweb_deeplinks 由 dns 模块进行统一管理，也由它提供相关的管理界面、控制策略
   */
  readonly dweb_deeplinks: $DWEB_DEEPLINK[];
  readonly categories: MICRO_MODULE_CATEGORY[];
}
export interface $MicroModuleRuntime extends $MicroModuleManifest {
  // nativeFetch(
  //   input: RequestInfo | URL,
  //   init?: RequestInit
  // ): Promise<Response> & typeof import("../helper/fetchExtends/index.ts")["fetchExtends"];
  connect(mmid: $MMID): Promise<import("./ipc/ipc.ts").Ipc | undefined>;

  /**
   * 添加双工连接到自己的池子中，但自己销毁，这些双工连接都会被断掉
   * @param ipc
   */
  beConnect(ipc: import("./ipc/ipc.ts").Ipc): Promise<void>;
}

/**ipc事件句柄 */
export const enum IPC_HANDLE_EVENT {
  Activity = "activity", // 激活应用程序时发出。各种操作都可以触发此事件，例如首次启动应用程序、在应用程序已运行时尝试重新启动该应用程序，或者单击应用程序的停靠栏或任务栏图标。
  Renderer = "renderer", // 窗口激活时发出，这里可以拿到应用的窗口句柄（wid）
  RendererDestroy = "renderer-destroy", // 窗口激活时发出，这里可以拿到应用的窗口句柄（wid）
  Shortcut = "shortcut", // dinamic quick action
}

// Base on @types/web-app-manifest 1.0.4
// Project: https://w3c.github.io/manifest/
// Base Definitions by: Yamagishi Kazutoshi <https://github.com/ykzts>
/**文本方向类型 */
export type TextDirectionType = "ltr" | "rtl" | "auto";
/**显示模式 */
export type DisplayModeType = "fullscreen" | "standalone" | "minimal-ui" | "browser";

/**
 * 每一个`ImageResource`代表了用于应用的图片，根据使用该对象的成员的语义，
 * 它适用于在各种上下文中使用（例如，作为应用菜单的一部分的图标等）。
 *
 * 参见：https://w3c.github.io/manifest/#imageresource-and-its-members
 */
export interface ImageResource {
  /**
   * `ImageResource`的`src`成员是用户代理可以获取图片数据的URL。
   *
   * 参见：https://w3c.github.io/manifest/#src-member
   */
  src: string;

  /**
   * ImageResource的`sizes`成员是由一组无序的唯一的空格分隔的令牌组成的字符串，
   * 代表图像的尺寸。
   *
   * 参见：https://w3c.github.io/manifest/#sizes-member
   */
  sizes?: string | undefined;

  /**
   * `ImageResource`的`type`成员是关于图像的MIME类型的提示。
   *
   * 参见：https://w3c.github.io/manifest/#type-member
   */
  type?: string | undefined;

  /**
   * `purpose`成员是一组无序的唯一的空格分隔的令牌，大小写不敏感。
   *
   * 参见：https://w3c.github.io/manifest/#purpose-member
   */
  purpose?: string | undefined;

  /**
   * `platform`成员表示包含对象适用于的平台。
   *
   * 参见：https://w3c.github.io/manifest/#platform-member
   */
  platform?: string | undefined;
}

/**
 * 每一个`Fingerprints`代表了用于验证应用程序的一组加密指纹。指纹有以下两个属性：`type`和`value`。
 *
 * 参见：https://w3c.github.io/manifest/#fingerprints-member
 */
export interface Fingerprint {
  type?: string | undefined;
  value?: string | undefined;
}

/**
 * “manifest”是一个包含启动参数和应用默认设置的JSON文档，
 * 它应用于当应用启动时的情况。
 *
 * 参见：https://w3c.github.io/manifest/#webappmanifest-dictionary
 */
export interface WebAppManifest {
  /**
   * “dir”成员指定manifest中那些能够设置方向性的成员的基本方向。
   *
   * 参见：https://w3c.github.io/manifest/#dir-member
   */
  dir?: TextDirectionType | undefined;

  /**
   * “lang”成员是一个语言标签（字符串），它指定了manifest中那些能够设置方向性的成员的主要语言
   * （因为知道语言也可以帮助设置方向）。
   *
   * 参见：https://w3c.github.io/manifest/#lang-member
   */
  lang?: string | undefined;

  /**
   * “name”成员是一个字符串，代表通常展示给用户看的应用的姓名
   * （如：在其他应用的清单里，或者作为图标的标签）。
   *
   * 参见：https://w3c.github.io/manifest/#name-member
   */
  name?: string | undefined;

  /**
   * “short_name”成员是一个字符串，代表应用姓名的缩写。
   *
   * 参见：https://w3c.github.io/manifest/#short_name-member
   */
  short_name?: string | undefined;

  /**
   * “description”成员让开发者有机会描述应用的用途。
   *
   * 参见：https://w3c.github.io/manifest/#description-member
   */
  description?: string | undefined;

  /**
   *
   * 参见：https://developer.chrome.com/docs/extensions/mv3/manifest/homepage_url/
   */
  homepage_url?: string | undefined;

  /**
   * “icons”成员是一个`ImageResource`s的数组，它可以在各种情境下作为应用的图标。
   *
   * 参见：https://w3c.github.io/manifest/#icons-member
   */
  icons?: ImageResource[] | undefined;

  /**
   * “screenshots”成员是一个`ImageResource`s的数组，它代表了应用在常见使用情景下的截图。
   *
   * 参见：https://w3c.github.io/manifest/#screenshots-member
   */
  screenshots?: ImageResource[] | undefined;

  /**
   * “categories”成员描述了预期的应用类别，也就是应用属于哪些类别。
   *
   * 参见：https://w3c.github.io/manifest/#categories-member
   */
  categories?: string[] | undefined;

  /**
   * “iarc_rating_id”成员是一个字符串，代表应用的国际年龄分级联盟（IARC）认证代码。
   *
   * 参见：https://w3c.github.io/manifest/#iarc_rating_id-member
   */
  iarc_rating_id?: string | undefined;

  /**
   * “start_url”成员是一个字符串，代表起始URL，也就是开发者希望用户启动应用时加载的URL
   * （如：当用户在设备的应用菜单或主屏幕上点击应用的图标时）。
   *
   * 参见：https://w3c.github.io/manifest/#start_url-member
   */
  start_url?: string | undefined;

  /**
   * “display”成员是一个`DisplayModeType`，它的值是显示模式值中的一个。
   *
   * 参见：https://w3c.github.io/manifest/#display-member
   */
  display?: DisplayModeType | undefined;

  /**
   * “orientation”成员是一个字符串，它作为应用所有顶级浏览内容的默认屏幕方向。
   *
   * 参见：https://w3c.github.io/manifest/#orientation-member
   */
  orientation?:
    | "any"
    | "landscape"
    | "landscape-primary"
    | "landscape-secondary"
    | "natural"
    | "portrait"
    | "portrait-primary"
    | "portrait-secondary"
    | undefined;

  /**
   * manifest的“id”成员是一个字符串，代表应用的身份。
   * 身份形式为URL，它与起始URL同源。
   *
   * 参见：https://w3c.github.io/manifest/#id-member
   */
  id?: string | undefined;

  /**
   * “theme_color”成员作为应用内容的默认主题颜色。
   *
   * 参见：https://w3c.github.io/manifest/#theme_color-member
   */
  theme_color?: string | undefined;

  /**
   * “background_color”成员描述了预期的应用背景颜色。
   *
   * 参见：https://w3c.github.io/manifest/#background_color-member
   */
  background_color?: string | undefined;

  /**
   * “scope”成员是一个字符串，代表这个应用的应用内容的导航范围。
   *
   * 参见：https://w3c.github.io/manifest/#scope-member
   */
  scope?: string | undefined;

  /**
   * “related_applications”成员列出了相关应用，提供了应用与相关应用间关系的提示。
   *
   * 参见：https://w3c.github.io/manifest/#related_applications-member
   */
  related_applications?: ExternalApplicationResource[] | undefined;

  /**
   * “prefer_related_applications”成员是一个布尔值，它作为一个提示给用户代理，表示是否应该优先选择相关应用而不是应用。
   *
   * 参见：https://w3c.github.io/manifest/#prefer_related_applications-member
   */
  prefer_related_applications?: boolean | undefined;

  /**
   * “shortcuts”成员是一个`ShortcutItem`s的数组，它们提供了应用内部主要任务的访问。
   *
   * 参见：https://w3c.github.io/manifest/#shortcuts-member
   */
  shortcuts?: ShortcutItem[] | undefined;
}

/**
 * 每一个`ExternalApplicationResources`代表了一个与应用相关的应用程序。
 *
 * 参见：https://w3c.github.io/manifest/#externalapplicationresource-and-its-members
 */
export interface ExternalApplicationResource {
  /**
   * `platform`成员表示包含对象适用于的平台。
   *
   * 参见：https://w3c.github.io/manifest/#platform-member-0
   */
  platform: string;

  /**
   * `ExternalApplicationResource`字典的`url`成员代表可以找到应用程序的URL。
   *
   * 参见：https://w3c.github.io/manifest/#url-member-0
   */
  url?: string | undefined;

  /**
   * `ExternalApplicationResource`字典的`id`成员代表用于在平台上代表应用程序的id。
   *
   * 参见：https://w3c.github.io/manifest/#id-member
   */
  id?: string | undefined;

  /**
   * `ExternalApplicationResource`字典的`min_version`成员代表与该应用相关的应用程序的最小版本。
   *
   * 参见：https://w3c.github.io/manifest/#min_version-member
   */
  min_version?: string | undefined;

  /**
   * `ExternalApplicationResource`字典的`fingerprints`成员代表`Fingerprint`s的数组。
   *
   * 参见：https://w3c.github.io/manifest/#fingerprints-member
   */
  fingerprints?: Fingerprint[] | undefined;
}

/**
 * 每一个`ShortcutItem`代表了一个链接，连接到网络应用内的一个关键任务或页面。
 *
 * 参见：https://w3c.github.io/manifest/#shortcutitem-and-its-members
 */
export interface ShortcutItem {
  /**
   * `ShortcutItem`的`name`成员是一个字符串，代表了快捷方式的名字，通常会在上下文菜单中显示给用户。
   *
   * 参见：https://w3c.github.io/manifest/#name-member-0
   */
  name: string;

  /**
   * `ShortcutItem`的`short_name`成员是一个字符串，代表了快捷方式名字的缩写。
   *
   * 参见：https://w3c.github.io/manifest/#short_name-member-0
   */
  short_name?: string | undefined;

  /**
   * `ShortcutItem`的`description`成员是一个字符串，允许开发者描述快捷方式的用途。
   *
   * 参见：https://w3c.github.io/manifest/#description-member-0
   */
  description?: string | undefined;

  /**
   * `ShortcutItem`的`url`成员是一个URL，在应用的范围内，当相关的快捷方式被激活时，它会被打开。
   *
   * 参见：https://w3c.github.io/manifest/#url-member
   */
  url: string;

  /**
   * `ShortcutItem`的`icons`成员是一个`ImageResource`的数组，它们可以在各种上下文中作为快捷方式的图标表示。
   *
   * 参见：https://w3c.github.io/manifest/#icons-member-0
   */
  icons?: ImageResource[] | undefined;
}
