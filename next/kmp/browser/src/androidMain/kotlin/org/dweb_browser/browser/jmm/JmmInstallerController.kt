package org.dweb_browser.browser.jmm

import io.ktor.utils.io.cancel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.browser.jmm.ui.JmmManagerViewHelper
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureString
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.std.dns.createActivity
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.consumeEachJsonLine
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.sys.window.ext.WindowBottomSheetsController
import org.dweb_browser.sys.window.ext.createBottomSheets

/**
 * JS 模块安装 的 控制器
 */
class JmmInstallerController(
  private val jmmNMM: JmmNMM,
  val jmmAppInstallManifest: JmmAppInstallManifest,
  val originUrl: String,
  // 一个jmmManager 只会创建一个task
  private var downloadTaskId: String?
) {
  private val openLock = Mutex()
  val viewModel: JmmManagerViewHelper = JmmManagerViewHelper(jmmAppInstallManifest, this)
  val ioAsyncScope = jmmNMM.ioAsyncScope

  private val downloadCompleteSignal = Signal<String>()
  val onDownloadComplete = downloadCompleteSignal.toListener()
  private val downloadStartSignal = Signal<TaskId>()
  val onDownloadStart = downloadCompleteSignal.toListener()

  private val viewDeferred = CompletableDeferred<WindowBottomSheetsController>()
  suspend fun getView() = viewDeferred.await()

  init {
    jmmNMM.ioAsyncScope.launch {
      jmmNMM.createBottomSheets() { modifier ->
        Render(modifier, this)
      }.also { viewDeferred.complete(it) }
    }
  }

  suspend fun openRender(hasNewVersion: Boolean) {
    /// 提供渲染适配
    val bottomSheets = getView()
    bottomSheets.setCloseTip("应用正在安装中")
    bottomSheets.open()
    viewModel.refreshStatus(hasNewVersion)
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
    // TODO 先查询当前的状态，如果存在
    val taskId = downloadTaskId?.let { "&taskId=$it" } ?: ""
    val fetchUrl = "file://download.browser.dweb/create?url=$metadataUrl$taskId"
    val response = jmmNMM.nativeFetch(fetchUrl)
    return response.text().also {
      this.downloadTaskId = it
      downloadStartSignal.emit(it)
    }
  }

  suspend fun getDownloadingTaskId(): String? {
    return downloadTaskId?.let { taskId ->
      if (jmmNMM.nativeFetch("file://download.browser.dweb/running?taskId=$taskId").boolean()) {
        taskId
      } else null
    }
  }

  fun watchProcess(taskId: TaskId, cb: suspend DownloadTask.() -> Unit) {
    jmmNMM.ioAsyncScope.launch {
      val res = jmmNMM.nativeFetch("file://download.browser.dweb/watch/progress?taskId=$taskId")
      val readChannel = res.stream().getReader("jmm watchProcess")
      readChannel.consumeEachJsonLine<DownloadTask> {
        it.cb()
        if (it.status.state == DownloadState.Completed) {
          // 关闭watchProcess
          readChannel.cancel()
          downloadCompleteSignal.emit(taskId)
          // 删除缓存的zip文件
          jmmNMM.remove(it.filepath)
        }
      }
    }
  }

  suspend fun start(): Boolean {
    val response = jmmNMM.nativeFetch("file://download.browser.dweb/start?taskId=$downloadTaskId")
    return response.boolean()
  }

  suspend fun pause(): Boolean {
    val response = jmmNMM.nativeFetch("file://download.browser.dweb/pause?taskId=$downloadTaskId")
    return response.boolean()
  }

  suspend fun cancel(): Boolean {
    val response = jmmNMM.nativeFetch("file://download.browser.dweb/cancel?taskId=$downloadTaskId")
    return response.boolean()
  }

  suspend fun decompress(task: DownloadTask): Boolean {
    var jmm = task.url.substring(task.url.lastIndexOf("/") + 1)
    jmm = jmm.substring(0, jmm.lastIndexOf("."))
    val sourcePath = jmmNMM.nativeFetch(buildUrlString("file://file.std.dweb/picker") {
      parameters.append("path", task.filepath)
    }).text()
    val targetPath = jmmNMM.nativeFetch(buildUrlString("file://file.std.dweb/picker") {
      parameters.append("path", "/data/apps/$jmm")
    }).text()
    return jmmNMM.nativeFetch(buildUrlString("file://zip.browser.dweb/decompress") {
      parameters.append("sourcePath", sourcePath)
      parameters.append("targetPath", targetPath)
    }).boolean().trueAlso {
      jmmNMM.nativeFetch(PureRequest(buildUrlString("file://file.std.dweb/write") {
        parameters.append("path", "/data/apps/$jmm/usr/sys/metadata.json")
        parameters.append("create", "true")
      }, IpcMethod.POST, body = IPureBody.from(Json.encodeToString(jmmAppInstallManifest))))
      jmmNMM.nativeFetch(PureRequest(buildUrlString("file://file.std.dweb/write") {
        parameters.append("path", "/data/apps/$jmm/usr/sys/session.json")
        parameters.append("create", "true")
      }, IpcMethod.POST, body = IPureBody.from(Json.encodeToString(buildJsonObject {
        put("installTime", JsonPrimitive(datetimeNow()))
        put("installUrl", JsonPrimitive(originUrl))
      }))))
    }
  }

  suspend fun closeSelf() {
    getView().close()
  }

  fun hasInstallApp() = jmmNMM.bootstrapContext.dns.query(jmmAppInstallManifest.id) != null
}