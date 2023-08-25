package info.bagen.dwebbrowser.microService.browser.jmm

import androidx.compose.runtime.Composable
import info.bagen.dwebbrowser.microService.browser.jmm.ui.JmmManagerViewModel
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.createWindowAdapterManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.SimpleSignal
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
  val viewModel: JmmManagerViewModel = JmmManagerViewModel(jmmAppInstallManifest, this)

  fun hasApps(mmid: MMID) = jmmNMM.getApps(mmid) !== null
  fun getApp(mmid: MMID) = jmmNMM.getApps(mmid)

  private val closeSignal = SimpleSignal()
  val onClosed = closeSignal.toListener()

  init {
    val wid = win.id
    /// 提供渲染适配
    createWindowAdapterManager.renderProviders[wid] =
      @Composable { modifier ->
        Render(modifier, this)
      }
    /// 窗口销毁的时候
    win.onClose {
      // 移除渲染适配器
      createWindowAdapterManager.renderProviders.remove(wid)
    }
    onClosed {
      win.close(true)
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
    debugJMM("closeApp", "mmid=$mmid")
    jmmNMM.bootstrapContext.dns.close(mmid)
  }

  suspend fun closeSelf() {
    closeSignal.emit()
  }
}