package org.dweb_browser.browser.jmm

import io.ktor.utils.io.cancel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.jmm.ui.JmmManagerViewHelper
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureString
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.std.dns.createActivity
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.consumeEachJsonLine
import org.dweb_browser.sys.window.ext.WindowBottomSheetsController
import org.dweb_browser.sys.window.ext.createBottomSheets

/**
 * JS 模块安装 的 控制器
 */
class JmmInstallerController(
  val jmmNMM: JmmNMM, jmmAppInstallManifest: JmmAppInstallManifest
) {

  // 一个jmmManager 只会创建一个task
  var taskId: String? = null
  private val openLock = Mutex()
  val viewModel: JmmManagerViewHelper = JmmManagerViewHelper(jmmAppInstallManifest, this)


  private val downloadCompletedSignal = Signal<String>()
  val onDownloadCompleted = downloadCompletedSignal.toListener()
  private val viewDeferred = CompletableDeferred<WindowBottomSheetsController>()
  suspend fun getView() = viewDeferred.await()

  suspend fun openRender() {
    /// 提供渲染适配
    val bottomSheets = jmmNMM.createBottomSheets() { modifier ->
      Render(modifier, this)
    }.also { viewDeferred.complete(it) }
    bottomSheets.setCloseTip("应用正在安装中")
    bottomSheets.open()
  }

  suspend fun openApp(mmid: MMID) {
    openLock.withLock {

      val (ipc) = jmmNMM.bootstrapContext.dns.connect(mmid)
      ipc.postMessage(IpcEvent.createActivity(""))

      val (deskIpc) = jmmNMM.bootstrapContext.dns.connect("desk.browser.dweb")
      debugJMM("openApp", "postMessage==>activity desk.browser.dweb")
      deskIpc.postMessage(IpcEvent.createActivity(""))
    }
  }

  suspend fun createDownloadTask(metadataUrl: String): PureString {
    val response = jmmNMM.nativeFetch("file://download.browser.dweb/create?url=$metadataUrl")
    return response.text().also { this.taskId = it }
  }

  fun watchProcess(taskId: String, cb: suspend DownloadTask.() -> Unit) {
    jmmNMM.ioAsyncScope.launch {
      val res = jmmNMM.nativeFetch("file://download.browser.dweb/watch/progress?taskId=$taskId")
      val readChannel = res.stream().getReader("jmm watchProcess")
      readChannel.consumeEachJsonLine<DownloadTask> {
        it.cb()
        if (it.status.state == DownloadState.Completed) {
          readChannel.cancel()
          downloadCompletedSignal.emit(taskId)
        }
      }
    }
  }

  suspend fun start(): Boolean {
    val response = jmmNMM.nativeFetch("file://download.browser.dweb/start?taskId=$taskId")
    return response.boolean()
  }

  suspend fun pause(): Boolean {
    val response = jmmNMM.nativeFetch("file://download.browser.dweb/pause?taskId=$taskId")
    return response.boolean()
  }

  suspend fun cancel(): Boolean {
    val response = jmmNMM.nativeFetch("file://download.browser.dweb/cancel?taskId=$taskId")
    return response.boolean()
  }

  suspend fun decompress(task: DownloadTask): Boolean {
    var jmm = task.url.substring(task.url.lastIndexOf("/") + 1)
    jmm = jmm.substring(0, jmm.lastIndexOf("."))
    val sourcePath = jmmNMM.nativeFetch("file://file.std.dweb/picker?path=${task.filepath}").text()
    val targetPath = jmmNMM.nativeFetch("file://file.std.dweb/picker?path=/data/apps/${jmm}").text()
    return jmmNMM.nativeFetch("file://zip.browser.dweb/decompress?sourcePath=$sourcePath&targetPath=$targetPath ")
      .boolean()
  }

  suspend fun closeSelf() {
    getView().close()
  }
}