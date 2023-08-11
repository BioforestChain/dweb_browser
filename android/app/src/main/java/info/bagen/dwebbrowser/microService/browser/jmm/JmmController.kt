package info.bagen.dwebbrowser.microService.browser.jmm

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import info.bagen.dwebbrowser.microService.browser.jmm.ui.JmmManagerViewModel
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.microservice.help.JmmAppInstallManifest
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.ipc.helper.IpcEvent

enum class EIpcEvent(val event: String) {
  State("state"), Ready("ready"), Activity("activity"), Close("close")
}

class JmmController(
  val win: WindowController, // 窗口控制器
  private val jmmNMM: JmmNMM,
  private val jmmAppInstallManifest: JmmAppInstallManifest
) {

  private val openLock = Mutex()
  val viewModel:JmmManagerViewModel = JmmManagerViewModel(jmmAppInstallManifest, this)

  fun hasApps(mmid: MMID) = jmmNMM.getApps(mmid) !== null
  fun getApp(mmid: MMID) = jmmNMM.getApps(mmid)

  init {
    val wid = win.id
    /// 提供渲染适配
    windowAdapterManager.providers[wid] =
      @Composable { modifier, width, height, scale ->
        Render(modifier, width, height, scale)
      }
    /// 窗口销毁的时候
    win.onClose {
      // 移除渲染适配器
      windowAdapterManager.providers.remove(wid)
    }
  }

  suspend fun openApp(mmid: MMID) {
    openLock.withLock {
      val (ipc) = jmmNMM.bootstrapContext.dns.connect(mmid)
      debugJMM("openApp", "postMessage==>activity ${ipc.remote.mmid}")
      ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))

      val (deskIpc) = jmmNMM.bootstrapContext.dns.connect("desk.browser.dweb")
      debugJMM("openApp", "postMessage==>activity desk.browser.dweb")
      deskIpc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
    }
  }

  suspend fun closeApp(mmid: MMID) {
    debugJMM("close APP", "postMessage==>close  $mmid")
    jmmNMM.bootstrapContext.dns.close(mmid)
  }
}