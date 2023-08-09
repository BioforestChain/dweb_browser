package info.bagen.dwebbrowser.microService.browser.desk

import info.bagen.dwebbrowser.microService.core.WindowState
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
