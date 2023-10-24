package org.dweb_browser.browser.jmm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
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
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.consumeEachJsonLine
import org.dweb_browser.sys.window.ext.WindowBottomSheetsController
import org.dweb_browser.sys.window.ext.createBottomSheets

class JmmController(
  internal val jmmNMM: JmmNMM, private val jmmAppInstallManifest: JmmAppInstallManifest
) {

  private val openLock = Mutex()
  val viewModel: JmmManagerViewHelper = JmmManagerViewHelper(jmmAppInstallManifest, this)

  fun getApp(mmid: MMID) = jmmNMM.getApps(mmid)


  internal val downloadSignal = Signal<JmmDownloadInfo>()
  val onDownload = downloadSignal.toListener()
  private val viewDeferred = CompletableDeferred<WindowBottomSheetsController>()
  suspend fun getView() = viewDeferred.await()

  init {
    jmmNMM.ioAsyncScope.launch {
      /// 提供渲染适配
      val bottomSheets = jmmNMM.createBottomSheets { modifier ->
        Render(modifier, this)
      }.also { viewDeferred.complete(it) }
      bottomSheets.open()
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

  fun watchProcess(taskId: String, cb: suspend DownloadTask.() -> Unit) {
    jmmNMM.ioAsyncScope.launch {
      val res = jmmNMM.nativeFetch("file://download.browser.dweb/watch/progress?taskId=$taskId")
      res.stream().getReader("jmm watchProcess").consumeEachJsonLine<DownloadTask> {
        it.cb()
      }
    }
  }

  suspend fun start(taskId: String): Boolean {
    val response = jmmNMM.nativeFetch("file://download.browser.dweb/start?taskId=$taskId")
    return response.boolean()
  }

  suspend fun unCompress(task: DownloadTask) {
    var jmm = task.url.substring(task.url.lastIndexOf("/") + 1)
    jmm = jmm.substring(0, jmm.lastIndexOf("."))
    jmmNMM.nativeFetch("file://file.std.dweb/unCompress?sourcePath=${task.filepath}&targetPath=/data/usr/${jmm}")
  }

  suspend fun updateDownloadState(downloadController: JmmDownloadController, mmid: MMID): Boolean {
    val url = when (downloadController) {
      JmmDownloadController.PAUSE -> "file://download.browser.dweb/pause?mmid=$mmid"
      JmmDownloadController.CANCEL -> "file://download.browser.dweb/cancel?mmid=$mmid"
      JmmDownloadController.RESUME -> "file://download.browser.dweb/start?mmid=$mmid"
    }
    return jmmNMM.nativeFetch(PureRequest(href = url, method = IpcMethod.GET)).boolean()
  }

  suspend fun closeSelf() {
    getView().close()
  }
}