package info.bagen.dwebbrowser.microService.browser.desktop

import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.ShortcutItem
import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.IpcSupportProtocols
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.help.MicroModuleManifest
import java.io.Serializable


data class DeskAppMetaData(
  var running: Boolean = false, // 是否正在运行
  var winStates: List<WindowState> = emptyList(), // 当前进程所拥有的窗口的状态
  var mmid: MMID = "",
  var background_color: String? = null,
  var categories: List<MICRO_MODULE_CATEGORY>? = null,
  var dir: String? = null,
  var description: String? = null,
  var display: DisplayMode? = DisplayMode.fullscreen,
  var icons: List<ImageResource>? = null,
  var dweb_deeplinks: List<DWEB_DEEPLINK> = listOf(),
  var ipc_support_protocols: IpcSupportProtocols = IpcSupportProtocols(
    cbor = false,
    protobuf = false,
    raw = false
  ),
  var name: String = "",
  var orientation: String? = "",
  var short_name: String? = null,
  var shortcuts: List<ShortcutItem> = listOf(),
  var lang: String? = "",
  var screenshots: List<ImageResource>? = null,
  var theme_color: String? = "",
  var version: String = ""
) : Serializable {

  fun setMetaData(metaData: MicroModuleManifest): DeskAppMetaData {
    this.mmid = metaData.mmid
    this.name = metaData.name
    this.short_name = metaData.short_name
    this.version = metaData.version
    this.description = metaData.description
    this.categories = metaData.categories
    this.dir = metaData.dir
    this.icons = metaData.icons
    this.screenshots = metaData.screenshots

    this.background_color = metaData.theme_color
    this.theme_color = metaData.theme_color

    this.dweb_deeplinks = metaData.dweb_deeplinks
    this.ipc_support_protocols = metaData.ipc_support_protocols

    this.orientation = metaData.orientation
    this.display = metaData.display
    this.shortcuts = metaData.shortcuts
    this.lang = metaData.lang

    return this
  }

//  enum class ScreenType {
//    Hide, Half, Full;
//  }
//
//  val sort: MutableState<Int> = mutableIntStateOf(0) // 排序，位置
//  val screenType: MutableState<ScreenType> = mutableStateOf(ScreenType.Hide) // 默认隐藏
//  val offsetX: MutableState<Float> = mutableFloatStateOf(0f) // X轴偏移量
//  val offsetY: MutableState<Float> = mutableFloatStateOf(0f) // Y轴偏移量
//  val zoom: MutableState<Float> = mutableFloatStateOf(1f) // 缩放
//
//  var viewItem: MultiWebViewController.MultiViewItem? = null

//  fun toJson():String {
//    return gson.toJson(jsMetaData).replace("{\"background_color\"","""{"isRunning":${isRunning},"isExpand":${isExpand},"background_color"""")
//  }
}

typealias Int = kotlin.Int
typealias Float = kotlin.Float

/**
 * 单个窗口的状态集
 */
data class WindowState(

  /**
   * 窗口位置和大小
   *
   * 窗口会被限制最小值,会被限制显示区域。
   * 终止,窗口最终会被绘制在用户可见可控的区域中
   */
  val bounds: Rectangle,

  /**
   * 是否全屏
   */
  val fullscreen: Boolean,

  /**
   * 是否最大化,如果全屏状态,那么该值也会同时为 true
   */
  val maximize: Boolean,

  /**
   * 是否最小化
   */
  val minimize: Boolean,

  /**
   * 当前是否缩放窗口
   */
  val resizable: Boolean,

  /**
   * 是否聚焦
   *
   * 目前只会有一个窗口被聚焦,未来实现多窗口联合显示的时候,就可能会有多个窗口同时focus,但这取决于所处宿主操作系统的支持。
   */
  val focus: Boolean,

  /**
   * 是否处于画中画模式
   */
  val pictureInPicture: Boolean,

  /**
   * 当前窗口层叠顺序
   * @types {float} 这里使用float,本质上意味着这个zIndex是一个“二维”值
   */
  val zIndex: Float,

  /**
   * 是否在闪烁提醒
   */
  val flashing: Boolean,

  /**
   * 闪烁的颜色
   */
  val flashColor: String,

  /**
   * 进度条
   */
  val progressBar: Float,

  /**
   * 是否置顶显示
   */
  val alwaysOnTop: Boolean,

  /**
   * 当前窗口所属的桌面
   */
  val desktopIndex: Int,

  /**
   * 当前窗口所在的屏幕
   */
  val screenIndex: Int

)

/**
 * 窗口的矩形边界
 */
data class Rectangle(

  val left: Int,

  val top: Int,

  val width: Int,

  val height: Int

)

