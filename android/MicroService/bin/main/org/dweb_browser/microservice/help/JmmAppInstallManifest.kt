package org.dweb_browser.microservice.help

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.ShortcutItem
import org.dweb_browser.helper.StringEnumSerializer

typealias MMID = String
typealias DWEB_DEEPLINK = String

/** Js模块应用 元数据 */
@Serializable
sealed class JmmAppManifest(
  val baseURI: String? = null,
  val server: MainServer = MainServer("/sys", "/server/plaoc.server.js"),
  override val dweb_deeplinks: List<DWEB_DEEPLINK> = listOf(),
  override val id: MMID = "",
  override val dir: String? = null,
  override val lang: String? = null,
  override val name: String = "", // 应用名称
  override val short_name: String = "", // 应用副标题
  override val description: String? = null,
  override val icons: List<ImageResource> = listOf(),
  override val screenshots: List<ImageResource>? = null,
  override val display: DisplayMode? = null,
  override val orientation: String? = null,
  override val categories: MutableList<MICRO_MODULE_CATEGORY> = mutableListOf(), // 应用类型
  override val theme_color: String? = null,
  override val background_color: String? = null,
  override val shortcuts: List<ShortcutItem> = listOf(),
  override var version: String = "0.0.1"
) : CommonAppManifest()

/** Js模块应用安装使用的元数据 */
@Serializable
data class JmmAppInstallManifest(
  /** 安装是展示用的 icon */
  val logo: String = "",
  /** 安装时展示用的截图 */
  val images: List<String> = emptyList(),
  var bundle_url: String = "",
  val bundle_hash: String = "",
  val bundle_size: Long = 0L,
  /**格式为 `hex:{signature}` */
  val bundle_signature: String = "",
  /**该链接必须使用和app-id同域名的网站链接，
   * 请求回来是一个“算法+公钥地址”的格式 "{algorithm}:hex;{publicKey}"，
   * 比如说 `rsa-sha256:hex;2...1` */
  val public_key_url: String = "",
  /** 安装时展示的作者信息 */
  val author: List<String> = emptyList(),
  /** 安装时展示的主页链接 */
  val home: String = "",
  /** 修改日志 */
  val change_log: String = "",
  /** 安装时展示的发布日期 */
  val release_date: String = "",
  /**
   * @deprecated 安装时显示的权限信息
   */
  val permissions: List<String> = emptyList(),
  /**
   * @deprecated 安装时显示的依赖模块
   */
  val plugins: List<String> = emptyList(),
  /**
   * 描述应用支持的语言，格式：http://www.lingoes.net/zh/translator/langcode.htm
   * en 英文，zh 中文
   */
  val languages: List<String> = emptyList()
) : JmmAppManifest()

@Serializable
data class MainServer(
  /**
   * 应用文件夹的目录
   */
  val root: String,
  /**
   * 入口文件
   */
  val entry: String
)

@Serializable
open class MicroModuleManifest(
  open val ipc_support_protocols: IpcSupportProtocols = IpcSupportProtocols(
    cbor = true, protobuf = true, raw = true
  ),
  open val mmid: MMID = "",
  override val dweb_deeplinks: List<DWEB_DEEPLINK> = listOf(),
  override val dir: String? = null,
  override val lang: String? = null,
  override val name: String = "", // 应用名称
  override val short_name: String = "", // 应用副标题
  override val description: String? = null,
  override val icons: List<ImageResource> = listOf(),
  override val screenshots: List<ImageResource>? = null,
  override val display: DisplayMode? = null,
  override val orientation: String? = null,
  override val categories: MutableList<MICRO_MODULE_CATEGORY> = mutableListOf(), // 应用类型
  override val theme_color: String? = null,
  override val background_color: String? = null,
  override val shortcuts: List<ShortcutItem> = listOf(),
  override var version: String = "0.0.1"
) : CommonAppManifest() {
  override val id: MMID get() = mmid
}

@Serializable
sealed class CommonAppManifest {
  abstract val dweb_deeplinks: List<DWEB_DEEPLINK>
  abstract val id: MMID
  abstract val dir: String?
  abstract val lang: String?
  abstract val name: String// 应用名称
  abstract val short_name: String // 应用副标题
  abstract val description: String?
  abstract val icons: List<ImageResource>
  abstract val screenshots: List<ImageResource>?
  abstract val display: DisplayMode?
  abstract val orientation: String?
  abstract val categories: MutableList<MICRO_MODULE_CATEGORY>// 应用类型
  abstract val theme_color: String?
  abstract val background_color: String?
  abstract val shortcuts: List<ShortcutItem>
  abstract var version: String
}

fun CommonAppManifest.toMicroModuleManifest(): MicroModuleManifest {
  if (this is MicroModuleManifest) return this
  return MicroModuleManifest(
    mmid = this.id,
    dweb_deeplinks = this.dweb_deeplinks,
    dir = this.dir,
    lang = this.lang,
    name = this.name,
    short_name = this.short_name,
    description = this.description,
    icons = this.icons,
    screenshots = this.screenshots,
    display = this.display,
    orientation = this.orientation,
    categories = this.categories,
    theme_color = this.theme_color,
    background_color = this.background_color,
    shortcuts = this.shortcuts,
    version = this.version
  )
}

@Serializable
data class IpcSupportProtocols(
  val cbor: Boolean,
  val protobuf: Boolean,
  val raw: Boolean,
)

object MICRO_MODULE_CATEGORY_Serializer : StringEnumSerializer<MICRO_MODULE_CATEGORY>(
  "MICRO_MODULE_CATEGORY",
  MICRO_MODULE_CATEGORY.ALL_VALUES,
  { type })

@Serializable(with = MICRO_MODULE_CATEGORY_Serializer::class)
enum class MICRO_MODULE_CATEGORY(val type: String) {

  //#region 1. Service 服务
  /** 服务大类
   * > 任何跟服务有关联的请填写该项,用于范索引
   * > 服务拥有后台运行的特征,如果不填写该项,那么程序可能会被当作普通应用程序被直接回收资源
   */
  Service("service"),

  //#region 1.1 内核服务
  /** 路由服务
   * > 通常指 `dns.std.dweb` 这个核心,它决策着模块之间通讯的路径
   */
  Routing_Service("routing-service"),

  /** 进程服务
   * > 提供python、js、wasm等语言的运行服务
   * > 和 计算服务 不同,进程服务通常是指 概念上运行在本地 的程序
   */
  Process_Service("process-service"),

  /** 渲染服务
   * > 可视化图形的能力
   * > 比如:Web渲染器、Terminal渲染器、WebGPU渲染器、WebCanvas渲染器 等
   */
  Render_Service("render-service"),

  /** 协议服务
   * > 比如 `http.std.dweb` 这个模块,提供 http/1.1 协议到 Ipc 的映射
   * > 比如 `bluetooth.std.dweb` 这个模块,提供了接口化的 蓝牙协议 管理
   */
  Protocol_Service("protocol-service"),

  /** 设备管理服务
   * > 通常指外部硬件设备
   * > 比如其它的计算机设备、或者通过蓝牙协议管理设备、键盘鼠标打印机等等
   */
  Device_Management_Service("device-management-service"),

  //#endregion

  //#region 1.2 基础服务

  /** 计算服务
   * > 通常指云计算平台所提供的服务,可以远程部署程序
   */
  Computing_Service("computing-service"),

  /** 存储服务
   * > 比如:文件、对象存储、区块存储
   * > 和数据库的区别是,它不会对存储的内容进行拆解,只能提供基本的写入和读取功能
   */
  Storage_Service("storage-service"),

  /** 数据库服务
   * > 比如:关系型数据库、键值数据库、时序数据库
   * > 和存储服务的区别是,它提供了一套接口来 写入数据、查询数据
   */
  Database_Service("database-service"),

  /** 网络服务
   * > 比如:网关、负载均衡
   */
  Network_Service("network-service"),

  //#endregion

  //#region 1.3 中间件服务

  /** 聚合服务
   * > 特征:服务编排、服务治理、统一接口、兼容转换
   * > 比如:聚合查询、分布式管理
   */
  Hub_Service("hub-service"),

  /** 分发服务
   * > 特征:减少网络访问的成本、提升网络访问的体验
   * > 比如:CDN、网络加速、文件共享
   */
  Distribution_Service("distribution-service"),

  /** 安全服务
   * > 比如:数据加密、访问控制
   */
  Security_Service("security-service"),

  //#endregion

  //#region 分析服务

  /** 日志服务 */
  Log_Service("log-service"),

  /** 指标服务 */
  Indicator_Service("indicator-service"),

  /** 追踪服务 */
  Tracking_Service("tracking-service"),

  //#endregion

  //#region 人工智能服务

  /** 视觉服务 */
  Visual_Service("visual-service"),

  /** 语音服务 */
  Audio_Service("audio-service"),

  /** 文字服务 */
  Text_Service("text-service"),

  /** 机器学习服务 */
  Machine_Learning_Service("machine-learning-service"),

  //#endregion

  //#region 2. Application 应用

  /** 应用 大类
   * > 如果存在应用特征的模块,都应该填写该项
   * > 应用特征意味着有可视化的图形界面模块,如果不填写该项,那么应用将无法被显示在用户桌面上
   */
  Application("application"),

  //#region 2.1 Application 应用 · 系统

  /**
   * 设置
   * > 通常指 `setting.std.dweb` 这个核心,它定义了一种模块管理的标准
   * > 通过这个标准,用户可以在该模块中聚合管理注册的模块
   * > 包括:权限管理、偏好管理、功能开关、主题与个性化、启动程序 等等
   * > 大部分 service 会它们的管理视图注册到该模块中
   */
  Settings("settings"),

  /** 桌面 */
  Desktop("desktop"),

  /** 网页浏览器 */
  Web_Browser("web-browser"),

  /** 文件管理 */
  Files("files"),

  /** 钱包 */
  Wallet("wallet"),

  /** 助理
   * > 该类应用通常拥有极高的权限,比如 屏幕阅读工具、AI助理工具 等
   */
  Assistant("assistant"),

  //#endregion

  //#region 2.2 Application 应用 · 工作效率

  /** 商业 */
  Business("business"),

  /** 开发者工具 */
  Developer("developer"),

  /** 教育 */
  Education("education"),

  /** 财务 */
  Finance("finance"),

  /** 办公效率 */
  Productivity("productivity"),

  /** 消息软件
   * > 讯息、邮箱
   */
  Messages("messages"),

  /** 实时互动 */
  Live("live"),

  //#endregion

  //#region 2.3 Application 应用 · 娱乐

  /** 娱乐 */
  Entertainment("entertainment"),

  /** 游戏 */
  Games("games"),

  /** 生活休闲 */
  Lifestyle("lifestyle"),

  /** 音乐 */
  Music("music"),

  /** 新闻 */
  News("news"),

  /** 体育 */
  Sports("sports"),

  /** 视频 */
  Video("video"),

  /** 照片 */
  Photo("photo"),

  //#endregion

  //#region 2.4 Application 应用 · 创意

  /** 图形和设计 */
  Graphics_a_Design("graphics-design"),

  /** 摄影与录像 */
  Photography("photography"),

  /** 个性化 */
  Personalization("personalization"),

  //#endregion

  //#region 2.5 Application 应用 · 实用工具

  /** 书籍 */
  Books("books"),

  /** 杂志 */
  Magazines("magazines"),

  /** 食物 */
  Food("food"),

  /** 健康 */
  Health("health"),

  /** 健身 */
  Fitness("fitness"),

  /** 医疗 */
  Medical("medical"),

  /** 导航 */
  Navigation("navigation"),

  /** 参考工具 */
  Reference("reference"),

  /** 实用工具 */
  Utilities("utilities"),

  /** 旅行 */
  Travel("travel"),

  /** 天气 */
  Weather("weather"),

  /** 儿童 */
  Kids("kids"),

  /** 购物 */
  Shopping("shopping"),

  /** 安全 */
  Security("security"),

  //#endregion

  //#region 2.6 Application 应用 · 社交

  Social("social"),

  /** 职业生涯 */
  Career("career"),

  /** 政府 */
  Government("government"),

  /** 政治 */
  Politics("politics"),

  //#endregion

  //#region 3. Game 游戏(属于应用的细分)

  /** 动作游戏 */
  Action_Games("action-games"),

  /** 冒险游戏 */
  Adventure_Games("adventure-games"),

  /** 街机游戏 */
  Arcade_Games("arcade-games"),

  /** 棋盘游戏 */
  Board_Games("board-games"),

  /** 卡牌游戏 */
  Card_Games("card-games"),

  /** 赌场游戏 */
  Casino_Games("casino-games"),

  /** 骰子游戏 */
  Dice_Games("dice-games"),

  /** 教育游戏 */
  Educational_Games("educational-games"),

  /** 家庭游戏 */
  Family_Games("family-games"),

  /** 儿童游戏 */
  Kids_Games("kids-games"),

  /** 音乐游戏 */
  Music_Games("music-games"),

  /** 益智游戏 */
  Puzzle_Games("puzzle-games"),

  /** 赛车游戏 */
  Racing_Games("racing-games"),

  /** 角色扮演游戏 */
  Role_Playing_Games("role-playing-games"),

  /** 模拟经营游戏 */
  Simulation_Games("simulation-games"),

  /** 运动游戏 */
  Sports_Games("sports-games"),

  /** 策略游戏 */
  Strategy_Games("strategy-games"),

  /** 问答游戏 */
  Trivia_Games("trivia-games"),

  /** 文字游戏 */
  Word_Games("word-games"), ;

  companion object {
    val ALL_VALUES = values().associateBy { it.type }
  }
}