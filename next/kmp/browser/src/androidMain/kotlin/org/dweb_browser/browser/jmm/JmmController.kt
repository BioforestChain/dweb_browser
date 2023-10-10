package org.dweb_browser.browser.jmm

import androidx.compose.runtime.Composable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.jmm.ui.JmmManagerViewHelper
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureStringBody
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.std.dns.createActivity
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.sys.download.JmmDownloadController
import org.dweb_browser.core.sys.download.JmmDownloadInfo
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.createWindowAdapterManager

class JmmController(
  val win: WindowController, // 窗口控制器
  internal val jmmNMM: JmmNMM, private val jmmAppInstallManifest: JmmAppInstallManifest
) {

  private val openLock = Mutex()
  val viewModel: JmmManagerViewHelper = JmmManagerViewHelper(jmmAppInstallManifest, this)

  fun getApp(mmid: MMID) = jmmNMM.getApps(mmid)


  internal val downloadSignal = Signal<JmmDownloadInfo>()
  val onDownload = downloadSignal.toListener()

  init {
    val wid = win.id
    /// 提供渲染适配
    createWindowAdapterManager.renderProviders[wid] = @Composable { modifier ->
      Render(modifier, this)
    }
  }

  suspend fun openApp(mmid: MMID) {
    openLock.withLock {

      val (ipc) = jmmNMM.bootstrapContext.dns.connect(mmid)
      ipc.postMessage(IpcEvent.createActivity(""))

      val (deskIpc) = jmmNMM.bootstrapContext.dns.connect("desk.browser.dweb")
      org.dweb_browser.browser.jmm.debugJMM("openApp", "postMessage==>activity desk.browser.dweb")
      deskIpc.postMessage(IpcEvent.createActivity(""))
    }
  }

  suspend fun downloadAndSaveZip(appInstallManifest: JmmAppInstallManifest) {
    jmmNMM.nativeFetch(
      PureRequest(
        href = "file://download.browser.dweb/download",
        method = IpcMethod.POST,
        body = PureStringBody(Json.encodeToString(appInstallManifest))
      )
    )
  }

  suspend fun updateDownloadState(downloadController: JmmDownloadController, mmid: MMID): Boolean {
    val url = when (downloadController) {
      JmmDownloadController.PAUSE -> "file://download.browser.dweb/pause?mmid=$mmid"
      JmmDownloadController.CANCEL -> "file://download.browser.dweb/cancel?mmid=$mmid"
      JmmDownloadController.RESUME -> "file://download.browser.dweb/resume?mmid=$mmid"
    }
    return jmmNMM.nativeFetch(PureRequest(href = url, method = IpcMethod.GET)).boolean()
  }

  suspend fun closeSelf() {
    win.close(true)
  }
}