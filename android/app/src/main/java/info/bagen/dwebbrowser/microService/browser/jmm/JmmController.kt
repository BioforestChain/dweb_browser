package info.bagen.dwebbrowser.microService.browser.jmm

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import info.bagen.dwebbrowser.microService.browser.jmm.render.Render
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.createWindowAdapterManager

enum class EIpcEvent(val event: String) {
  State("state"), Ready("ready"), Activity("activity"), Close("close")
}

class JmmController(
  private val jmmNMM: JmmNMM,
  private val context: Context,
//  private val scope: CoroutineScope
) {

  private val openLock = Mutex()
  val managerApps = JmmManagerController()
  val installingApps =
    mutableStateMapOf<String, JmmAppInstallController>()//  = JmmManagerViewHelper(jmmAppInstallManifest, this)

  fun hasApps(mmid: MMID) = jmmNMM.getApps(mmid) !== null
  fun getApp(mmid: MMID) = jmmNMM.getApps(mmid)

  private val closeSignal = SimpleSignal()
  val onClosed = closeSignal.toListener()
  val routeToPath = mutableStateOf<String?>(null)

  fun bindWindow(win: WindowController) {
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
    /// 监听关闭信号，如果触发，那么关闭窗口
    onClosed {
      win.close(true)
    }
  }

  suspend fun openApp(mmid: MMID) {
    openLock.withLock {

      val (ipc) = jmmNMM.bootstrapContext.dns.connect(mmid)
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
    closeSignal.emitAndClear()
  }
}