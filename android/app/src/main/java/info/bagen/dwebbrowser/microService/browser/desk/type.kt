package info.bagen.dwebbrowser.microService.browser.desk

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.ShortcutItem
import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.IpcSupportProtocols
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.help.MicroModuleManifest
import org.dweb_browser.window.core.WindowState
@Serializable
data class DeskLinkMetaData(
  val title: String,
  val icon: ImageResource? = null,
  val url: String
)

@Serializable
data class DeskAppMetaData(
  var running: Boolean = false, // 是否正在运行
  var winStates: List<WindowState> = emptyList(), // 当前进程所拥有的窗口的状态
  val mmid: MMID = "",
  val background_color: String? = null,
  val categories: MutableList<MICRO_MODULE_CATEGORY> = mutableListOf(),
  val dir: String? = null,
  val description: String? = null,
  val display: DisplayMode? = DisplayMode.Fullscreen,
  val icons: List<ImageResource> = listOf(),
  val dweb_deeplinks: List<DWEB_DEEPLINK> = listOf(),
  val ipc_support_protocols: IpcSupportProtocols = IpcSupportProtocols(
    cbor = false, protobuf = false, raw = false
  ),
  val name: String = "",
  val orientation: String? = "",
  val short_name: String = "",
  val shortcuts: List<ShortcutItem> = listOf(),
  val lang: String? = "",
  val screenshots: List<ImageResource>? = null,
  val theme_color: String? = "",
  var version: String = ""
) {
  constructor(
    running: Boolean,
    winStates: List<WindowState> = emptyList(),
    parent: MicroModuleManifest
  ) : this(
    running = running,
    winStates = winStates,
    mmid = parent.mmid,
    background_color = parent.background_color,
    categories = parent.categories,
    dir = parent.dir,
    description = parent.description,
    display = parent.display,
    icons = parent.icons,
    dweb_deeplinks = parent.dweb_deeplinks,
    ipc_support_protocols = parent.ipc_support_protocols,
    name = parent.name,
    orientation = parent.orientation,
    short_name = parent.short_name,
    shortcuts = parent.shortcuts,
    lang = parent.lang,
    screenshots = parent.screenshots,
    theme_color = parent.theme_color,
    version = parent.version
  )
}