package org.dweb_browser.browser.jmm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.browser.jmm.model.JmmInstallerModel
import org.dweb_browser.browser.jmm.ui.Render
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.sys.window.core.constant.LowLevelWindowAPI
import org.dweb_browser.sys.window.core.modal.WindowBottomSheetsController
import org.dweb_browser.sys.window.ext.createBottomSheets
import org.dweb_browser.sys.window.ext.getOrOpenMainWindow

/**
 * JS 模块安装 的 控制器
 */
class JmmInstallerController(
  private val jmmNMM: JmmNMM,
  private val jmmHistoryMetadata: JmmHistoryMetadata,
  private val jmmController: JmmController,
  private val openFromHistory: Boolean,
) {
  val viewModel: JmmInstallerModel = JmmInstallerModel(jmmHistoryMetadata, this)
  val ioAsyncScope = jmmNMM.ioAsyncScope

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

  suspend fun openRender() {
    /// 隐藏主窗口
    if (!openFromHistory) {
      jmmNMM.getOrOpenMainWindow().hide()
    }
    /// 显示抽屉
    val bottomSheets = getView()
    bottomSheets.open()
    bottomSheets.onClose {
      /// TODO 如果应用正在下载，则显示toast应用正在安装中
    }
  }

  suspend fun openApp(mmid: MMID) =
    jmmNMM.nativeFetch("file://desk.browser.dweb/openAppOrActivate?app_id=$mmid")

  /**
   * 创建任务，如果存在则恢复
   */
  suspend fun createDownloadTask() = jmmController.createDownloadTask(jmmHistoryMetadata)

  suspend fun start() = jmmController.start(jmmHistoryMetadata)

  suspend fun pause() = jmmController.pause(jmmHistoryMetadata.taskId)

  suspend fun cancel() = jmmController.cancel(jmmHistoryMetadata.taskId)

  suspend fun exists() = jmmController.exists(jmmHistoryMetadata.taskId)

  suspend fun closeSelf() {
    jmmNMM.getOrOpenMainWindow().closeRoot()
  }
}