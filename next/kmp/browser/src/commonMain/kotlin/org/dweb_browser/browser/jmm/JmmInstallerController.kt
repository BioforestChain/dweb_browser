package org.dweb_browser.browser.jmm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.browser.jmm.model.JmmInstallerModel
import org.dweb_browser.browser.jmm.model.JmmStatus
import org.dweb_browser.browser.jmm.ui.Render
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureString
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Signal
import org.dweb_browser.sys.window.core.modal.WindowBottomSheetsController
import org.dweb_browser.sys.window.ext.createBottomSheets
import org.dweb_browser.sys.window.ext.getOrOpenMainWindow

/**
 * JS 模块安装 的 控制器
 */
class JmmInstallerController(
  private val jmmNMM: JmmNMM,
  val jmmHistoryMetadata: JmmHistoryMetadata,/*
  private val originUrl: String,
  val jmmAppInstallManifest: JmmAppInstallManifest,
  // 一个jmmManager 只会创建一个task
  var downloadTaskId: String?,*/
  private val store: JmmStore,
  private val jmmController: JmmController
) {
  val viewModel: JmmInstallerModel = JmmInstallerModel(jmmHistoryMetadata.metadata, this)
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

  suspend fun openApp(mmid: MMID) =
    jmmNMM.nativeFetch("file://desk.browser.dweb/openAppOrActivate?app_id=$mmid")

  /**
   * 创建任务，如果存在则恢复
   */
  suspend fun createDownloadTask(metadataUrl: String, total: Long): PureString =
    jmmController.createDownloadTask(metadataUrl, total).also {
      // jmmStateSignal.emit(Pair(JmmStatus.Init, it)) // 创建下载任务时，保存进度
      jmmHistoryMetadata.taskId = it
      jmmHistoryMetadata.initTaskId(it, store)
    }

  suspend fun watchProcess(callback: suspend (JmmStatus, Long, Long) -> Unit) {
    val taskId = jmmHistoryMetadata.taskId ?: return
    jmmController.watchProcess(jmmHistoryMetadata) { state, current, total ->
      when (state) {
        JmmStatus.Init -> jmmStateSignal.emit(Pair(JmmStatus.Init, taskId))
        JmmStatus.Completed -> jmmStateSignal.emit(Pair(JmmStatus.Completed, taskId))
        JmmStatus.INSTALLED -> jmmStateSignal.emit(Pair(JmmStatus.INSTALLED, taskId))
        else -> {}
      }
      callback(state, current, total)
    }
  }

  suspend fun start() = jmmController.start(jmmHistoryMetadata.taskId)

  suspend fun pause() = jmmController.pause(jmmHistoryMetadata.taskId)

  suspend fun cancel() = jmmController.cancel(jmmHistoryMetadata.taskId)

  suspend fun exists() = jmmController.exists(jmmHistoryMetadata.taskId)

  suspend fun closeSelf() {
    getView().close()
  }

  fun hasInstallApp() = jmmNMM.bootstrapContext.dns.query(jmmHistoryMetadata.metadata.id) != null
}