package org.dweb_browser.browser.jmm

import androidx.compose.runtime.Composable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.jmm.ui.JmmManagerViewHelper
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureString
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.std.dns.createActivity
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.sys.download.JmmDownloadController
import org.dweb_browser.core.sys.download.JmmDownloadInfo
import org.dweb_browser.helper.ChangeState
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.consumeEachJsonLine
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

  suspend fun createDownloadTask(metadataUrl: String): PureString {
    val response = jmmNMM.nativeFetch("file://download.browser.dweb/create?url=$metadataUrl")
    return response.text()
  }

  suspend fun watchProcess(taskId:String, cb: suspend DownloadTask.() -> Unit) {
   val res =  jmmNMM.nativeFetch("file://download.browser.dweb/watch/progress?taskId=$taskId")
    res.stream().getReader("jmm watchProcess").consumeEachJsonLine<DownloadTask> {
      it.cb()
    }
  }

  suspend fun unCompress(task: DownloadTask) {
     var jmm = task.url.substring(task.url.lastIndexOf("/")+1)
    jmm = jmm.substring(0,jmm.lastIndexOf("."))
    jmmNMM.nativeFetch("file://file.std.dweb/unCompress?sourcePath=${task.filepath}&targetPath=/data/usr/${jmm}")
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