package org.dweb_browser.helper

import java.io.Serializable

typealias Mmid = String
typealias DWEB_DEEPLINK = String

data class AppMetaData(
  val id: Mmid, // jmmApp的id
  val server: MainServer = MainServer("/sys", "/server/plaoc.server.js"), // 打开应用地址
  val dweb_deeplinks: MutableList<DWEB_DEEPLINK> = mutableListOf(),
  val name: String = "", // 应用名称
  val short_name: String = "", // 应用副标题
  val icon: String = "", // 应用图标
  val images: List<String>? = null, // 应用截图
  val description: String = "", // 应用描述
  val author: List<String>? = null, // 开发者，作者
  val categories: MutableList<MicroModuleCategory> = mutableListOf(), // 应用类型
  val version: String = "", // 应用版本
  val new_feature: String? = null, // 新特性，新功能
  val home: String = "", // 首页地址
  var bundle_url: String = "", // 下载应用地址
  val bundle_size: String = "", // 应用大小
  val bundle_hash: String = "", // 文件hash
  val permissions: List<String>? = null, // app使用权限的情况
  val plugins: List<String>? = null, // app使用插件的情况
  val release_date: String = "", // 发布时间
  val isRunning: Boolean = false, // 是否正在运行
  val isExpand: Boolean = false, // 是否默认展开窗口
) : Serializable {
  data class MainServer(
    /**
     * 应用文件夹的目录
     */
    val root: String,
    /**
     * 入口文件
     */
    val entry: String
  ) : Serializable
}

enum class MicroModuleCategory {

  //#region 1. Service 服务
  /** 服务大类
   * > 任何跟服务有关联的请填写该项,用于范索引
   * > 服务拥有后台运行的特征,如果不填写该项,那么程序可能会被当作普通应用程序被直接回收资源
   */
  Service,

  //#region 1.1 内核服务
  /** 路由服务
   * > 通常指 `dns.std.dweb` 这个核心,它决策着模块之间通讯的路径
   */
  Routing_Service,

  /** 进程服务
   * > 提供python、js、wasm等语言的运行服务
   * > 和 计算服务 不同,进程服务通常是指 概念上运行在本地 的程序
   */
  Process_Service,

  /** 协议服务
   * > 比如 `http.std.dweb` 这个模块,提供 http/1.1 协议到 Ipc 的映射
   * > 比如 `bluetooth.std.dweb` 这个模块,提供了接口化的 蓝牙协议 管理
   */
  Protocol_Service,

  /** 设备管理服务
   * > 通常指外部硬件设备
   * > 比如其它的计算机设备、或者通过蓝牙协议管理设备、键盘鼠标打印机等等
   */
  Device_Management_Service,

  //#endregion

  //#region 1.2 基础服务

  /** 计算服务
   * > 通常指云计算平台所提供的服务,可以远程部署程序
   */
  Computing_Service,

  /** 存储服务
   * > 比如:文件、对象存储、区块存储
   * > 和数据库的区别是,它不会对存储的内容进行拆解,只能提供基本的写入和读取功能
   */
  Storage_Service,

  /** 数据库服务
   * > 比如:关系型数据库、键值数据库、时序数据库
   * > 和存储服务的区别是,它提供了一套接口来 写入数据、查询数据
   */
  Database_Service,

  /** 网络服务
   * > 比如:网关、负载均衡
   */
  Network_Service,

  //#endregion

  //#region 1.3 中间件服务

  /** 聚合服务
   * > 特征:服务编排、服务治理、统一接口、兼容转换
   * > 比如:聚合查询、分布式管理
   */
  Hub_Service,

  /** 分发服务
   * > 特征:减少网络访问的成本、提升网络访问的体验
   * > 比如:CDN、网络加速、文件共享
   */
  Distribution_Service,

  /** 安全服务
   * > 比如:数据加密、访问控制
   */
  Security_Service,

  //#endregion

  //#region 分析服务

  /** 日志服务 */
  Log_Service,

  /** 指标服务 */
  Indicator_Service,

  /** 追踪服务 */
  Tracking_Service,

  //#endregion

  //#region 人工智能服务

  /** 视觉服务 */
  Visual_Service,

  /** 语音服务 */
  Audio_Service,

  /** 文字服务 */
  Text_Service,

  /** 机器学习服务 */
  Machine_Learning_Service,

  //#endregion

  //#region 2. Application 应用

  /** 应用 大类
   * > 如果存在应用特征的模块,都应该填写该项
   * > 应用特征意味着有可视化的图形界面模块,如果不填写该项,那么应用将无法被显示在用户桌面上
   */
  Application,

  //#region 2.1 Application 应用 · 系统

  /**
   * 设置
   * > 通常指 `setting.std.dweb` 这个核心,它定义了一种模块管理的标准
   * > 通过这个标准,用户可以在该模块中聚合管理注册的模块
   * > 包括:权限管理、偏好管理、功能开关、主题与个性化、启动程序 等等
   * > 大部分 service 会它们的管理视图注册到该模块中
   */
  Settings,

  /** 桌面 */
  Desktop,

  /** 网页浏览器 */
  Web_Browser,

  /** 文件管理 */
  Files,

  /** 钱包 */
  Wallet,

  /** 助理
   * > 该类应用通常拥有极高的权限,比如 屏幕阅读工具、AI助理工具 等
   */
  Assistant,

  //#endregion

  //#region 2.2 Application 应用 · 工作效率

  /** 商业 */
  Business,

  /** 开发者工具 */
  Developer,

  /** 教育 */
  Education,

  /** 财务 */
  Finance,

  /** 办公效率 */
  Productivity,

  /** 消息软件
   * > 讯息、邮箱
   */
  Messages,

  /** 实时互动 */
  Live,

  //#endregion

  //#region 2.3 Application 应用 · 娱乐

  /** 娱乐 */
  Entertainment,

  /** 游戏 */
  Games,

  /** 生活休闲 */
  Lifestyle,

  /** 音乐 */
  Music,

  /** 新闻 */
  News,

  /** 体育 */
  Sports,

  /** 视频 */
  Video,

  /** 照片 */
  Photo,

  //#endregion

  //#region 2.4 Application 应用 · 创意

  /** 图形和设计 */
  Graphics_a_Design,

  /** 摄影与录像 */
  Photography,

  /** 个性化 */
  Personalization,

  //#endregion

  //#region 2.5 Application 应用 · 实用工具

  /** 书籍 */
  Books,

  /** 杂志 */
  Magazines,

  /** 食物 */
  Food,

  /** 健康 */
  Health,

  /** 健身 */
  Fitness,

  /** 医疗 */
  Medical,

  /** 导航 */
  Navigation,

  /** 参考工具 */
  Reference,

  /** 实用工具 */
  Utilities,

  /** 旅行 */
  Travel,

  /** 天气 */
  Weather,

  /** 儿童 */
  Kids,

  /** 购物 */
  Shopping,

  /** 安全 */
  Security,

  //#endregion

  //#region 2.6 Application 应用 · 社交
  Social,

  /** 职业生涯 */
  Career,

  /** 政府 */
  Government,

  /** 政治 */
  Politics,

  //#endregion

  //#region 3. Game 游戏(属于应用的细分)

  /** 动作游戏 */
  Action_Games,

  /** 冒险游戏 */
  Adventure_Games,

  /** 街机游戏 */
  Arcade_Games,

  /** 棋盘游戏 */
  Board_Games,

  /** 卡牌游戏 */
  Card_Games,

  /** 赌场游戏 */
  Casino_Games,

  /** 骰子游戏 */
  Dice_Games,

  /** 教育游戏 */
  Educational_Games,

  /** 家庭游戏 */
  Family_Games,

  /** 儿童游戏 */
  Kids_Games,

  /** 音乐游戏 */
  Music_Games,

  /** 益智游戏 */
  Puzzle_Games,

  /** 赛车游戏 */
  Racing_Games,

  /** 角色扮演游戏 */
  Role_Playing_Games,

  /** 模拟经营游戏 */
  Simulation_Games,

  /** 运动游戏 */
  Sports_Games,

  /** 策略游戏 */
  Strategy_Games,

  /** 问答游戏 */
  Trivia_Games,

  /** 文字游戏 */
  Word_Games,

  //#endregion

}