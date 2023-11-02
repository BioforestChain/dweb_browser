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
import org.dweb_browser.browser.jmm.ui.JmmStatus
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureString
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.std.dns.ext.createActivity
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.consumeEachJsonLine
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.sys.window.core.modal.WindowBottomSheetsController
import org.dweb_browser.sys.window.ext.createBottomSheets
import org.dweb_browser.sys.window.ext.createRenderer
import org.dweb_browser.sys.window.ext.getOrOpenMainWindow
import org.dweb_browser.sys.window.ext.openMainWindow

/**
 * JS 模块安装 的 控制器
 */
class JmmInstallerController(
  private val jmmNMM: JmmNMM,
  private val originUrl: String,
  val jmmAppInstallManifest: JmmAppInstallManifest,
  // 一个jmmManager 只会创建一个task
  var downloadTaskId: String?
) {
  private val openLock = Mutex()
  val viewModel: JmmManagerViewHelper = JmmManagerViewHelper(jmmAppInstallManifest, this)
  val ioAsyncScope = jmmNMM.ioAsyncScope

  private val jmmStateSignal = Signal<Pair<JmmStatus, TaskId>>()
  val onJmmStateListener = jmmStateSignal.toListener()

  private val viewDeferred = CompletableDeferred<WindowBottomSheetsController>()
  suspend fun getView() = viewDeferred.await()

  init {
    jmmNMM.ioAsyncScope.launch {
      /// 创建 BottomSheets 视图，提供渲染适配
      jmmNMM.createBottomSheets() { modifier ->
        Render(modifier, this)
      }.also { viewDeferred.complete(it) }
    }
  }

  suspend fun openRender(hasNewVersion: Boolean) {
    /// 隐藏主窗口
    jmmNMM.getOrOpenMainWindow().hide()
    /// 显示抽屉
    val bottomSheets = getView()
    bottomSheets.open()
    bottomSheets.onClose {
      /// TODO 如果应用正在下载，则显示toast应用正在安装中
    }
    viewModel.refreshStatus(hasNewVersion)
  }

  suspend fun openApp(mmid: MMID) {
    jmmNMM.nativeFetch("file://desk.browser.dweb/openAppOrActivate?app_id=$mmid")
    /*openLock.withLock {
      val (ipc) = jmmNMM.bootstrapContext.dns.connect(mmid)
      ipc.postMessage(IpcEvent.createActivity(""))

      val (deskIpc) = jmmNMM.bootstrapContext.dns.connect("desk.browser.dweb")
      debugJMM("openApp", "postMessage==>activity desk.browser.dweb")
      deskIpc.postMessage(IpcEvent.createActivity(""))
    }*/
  }

  /**
   * 创建任务，如果存在则恢复
   */
  suspend fun createDownloadTask(metadataUrl: String, total: Long): PureString {
    val fetchUrl = "file://download.browser.dweb/create?url=$metadataUrl&total=$total"
    val response = jmmNMM.nativeFetch(fetchUrl)
    return response.text().also {
      jmmStateSignal.emit(Pair(JmmStatus.Init, it)) // 创建下载任务时，保存进度
      this.downloadTaskId = it
    }
  }

  suspend fun watchProcess(taskId: TaskId, callback: suspend (JmmStatus, Long, Long) -> Unit) {
    jmmNMM.ioAsyncScope.launch {
      val res = jmmNMM.nativeFetch("file://download.browser.dweb/watch/progress?taskId=$taskId")
      val readChannel = res.stream().getReader("jmm watchProcess")
      readChannel.consumeEachJsonLine<DownloadTask> { downloadTask ->
        when (downloadTask.status.state) {
          DownloadState.Init -> {
            callback(JmmStatus.Init, 0L, downloadTask.status.total)
            jmmStateSignal.emit(Pair(JmmStatus.Init, downloadTask.id))
          }

          DownloadState.Downloading -> {
            callback(JmmStatus.Downloading, downloadTask.status.current, downloadTask.status.total)
          }

          DownloadState.Paused -> {
            callback(JmmStatus.Paused, downloadTask.status.current, downloadTask.status.total)
          }

          DownloadState.Canceled -> {
            callback(JmmStatus.Canceled, downloadTask.status.current, downloadTask.status.total)
          }

          DownloadState.Failed -> {
            callback(JmmStatus.Failed, 0L, downloadTask.status.total)
          }

          DownloadState.Completed -> {
            callback(JmmStatus.Completed, downloadTask.status.current, downloadTask.status.total)
            jmmStateSignal.emit(Pair(JmmStatus.Completed, downloadTask.id))
            if (decompress(downloadTask)) {
              callback(JmmStatus.INSTALLED, downloadTask.status.current, downloadTask.status.total)
              jmmStateSignal.emit(Pair(JmmStatus.INSTALLED, downloadTask.id))
            } else {
              callback(JmmStatus.Failed, 0L, downloadTask.status.total)
            }
            // 关闭watchProcess
            readChannel.cancel()
            // 删除缓存的zip文件
            jmmNMM.remove(downloadTask.filepath)
          }
        }
      }
    }
  }

  suspend fun start() = downloadTaskId?.let { taskId ->
    jmmNMM.nativeFetch("file://download.browser.dweb/start?taskId=$taskId").boolean()
  } ?: false

  suspend fun pause() = downloadTaskId?.let { taskId ->
    jmmNMM.nativeFetch("file://download.browser.dweb/pause?taskId=$taskId").boolean()
  } ?: false

  suspend fun cancel() = downloadTaskId?.let { taskId ->
    jmmNMM.nativeFetch("file://download.browser.dweb/cancel?taskId=$taskId").boolean()
  } ?: false

  suspend fun exists() = downloadTaskId?.let { taskId ->
    jmmNMM.nativeFetch("file://download.browser.dweb/exists?taskId=$taskId").boolean()
  } ?: false

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
      // 保存 session（记录安装时间） 和 metadata （app数据源）
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