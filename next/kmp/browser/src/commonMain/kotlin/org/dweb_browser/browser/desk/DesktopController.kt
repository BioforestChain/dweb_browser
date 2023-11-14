package org.dweb_browser.browser.desk

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import org.dweb_browser.browser.desk.types.DeskAppMetaData
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.build
import org.dweb_browser.helper.resolvePath

expect fun IDesktopController.Companion.create(
  deskNMM: DeskNMM,
  desktopServer: HttpDwebServer,
  runningApps: ChangeableMap<MMID, RunningApp>
): IDesktopController

@Stable
abstract class IDesktopController(
  private val deskNMM: DeskNMM,
  private val desktopServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, RunningApp>
) {
  companion object {}

  internal val updateSignal = SimpleSignal()
  val onUpdate = updateSignal.toListener()

  init {
    runningApps.onChange {
      updateSignal.emit()
    }
  }

  suspend fun getDesktopApps(): List<DeskAppMetaData> {
    val apps = deskNMM.bootstrapContext.dns.search(MICRO_MODULE_CATEGORY.Application)
    val runApps = apps.map { metaData ->
      return@map DeskAppMetaData().apply {
        running = runningApps.containsKey(metaData.mmid)
        winStates = desktopWindowsManager.getWindowStates(metaData.mmid)
        winStates.firstOrNull()?.let { state ->
          debugDesk(
            "getDesktopApps", "winStates -> ${winStates.size}, ${state.mode}, ${state.focus}"
          )
        }
        assign(metaData.manifest)
      }
    }
    return runApps
  }

  protected var preDesktopWindowsManager: DesktopWindowsManager? = null

  /**
   * 窗口管理器
   */
  abstract val desktopWindowsManager: DesktopWindowsManager

  fun getDesktopUrl() = desktopServer.startResult.urlInfo.buildInternalUrl().build {
    resolvePath("/desktop.html")
    parameters["api-base"] = desktopServer.startResult.urlInfo.buildPublicUrl().toString()
  }


  protected val _activitySignal = SimpleSignal()
  val onActivity = _activitySignal.toListener()


  data class AlertMessage(val title: String, val message: String)

  internal val alertMessages = mutableStateListOf<AlertMessage>()
  fun showAlert(reason: Throwable) {
    val title = reason.cause?.message ?: "异常"
    val message = reason.message ?: "未知原因"
    alertMessages.add(AlertMessage(title, message))
  }
}
