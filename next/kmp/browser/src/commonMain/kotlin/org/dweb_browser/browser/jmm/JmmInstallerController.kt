package org.dweb_browser.browser.jmm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.ui.Render
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.sys.window.core.modal.WindowBottomSheetsController

internal val LocalShowWebViewVersion = compositionChainOf("ShowWebViewVersion") {
  mutableStateOf(false)
}

internal val LocalJmmInstallerController =
  compositionChainOf<JmmInstallerController>("JmmInstallerController")

/**
 * JS 模块安装 的 控制器
 */
class JmmInstallerController(
  private val jmmNMM: JmmNMM,
  initJmmHistoryMetadata: JmmHistoryMetadata,
  private val jmmController: JmmController,
  private val openFromHistory: Boolean,
) {
  var jmmHistoryMetadata by ObservableMutableState(initJmmHistoryMetadata) {}
    internal set

  val ioAsyncScope = jmmNMM.ioAsyncScope

  private var viewDeferred = CompletableDeferred<WindowBottomSheetsController>()
  private val getViewLock = Mutex()

  @OptIn(ExperimentalCoroutinesApi::class)
  suspend fun getView() = getViewLock.withLock {
    if (viewDeferred.isCompleted) {
      val bottomSheetsModal = viewDeferred.getCompleted()
      /// TODO 这里 onDestroy 回调可能不触发，因此需要手动进行一次判断
      if (bottomSheetsModal.wid == jmmNMM.getMainWindowId()) {
        return@withLock bottomSheetsModal
      }
      viewDeferred = CompletableDeferred()
    }
    /// 创建 BottomSheets 视图，提供渲染适配
    jmmNMM.createBottomSheets() { modifier ->
      Render(modifier, this)
    }.also {
      viewDeferred.complete(it)
      it.onDestroy {
        viewDeferred = CompletableDeferred()
      }
    }
  } // viewDeferred.await()

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

  suspend fun openApp() {
    jmmNMM.nativeFetch("file://desk.browser.dweb/openAppOrActivate?app_id=${jmmHistoryMetadata.metadata.id}")
    closeSelf() // 打开应用后，需要关闭当前安装界面
  }

  // 关闭原来的app
  suspend fun closeApp() {
    jmmNMM.nativeFetch("file://desk.browser.dweb/closeApp?app_id=${jmmHistoryMetadata.metadata.id}")
  }

  private val mutexLock = Mutex()

  /**
   * 创建任务，如果存在则恢复
   */
  suspend fun createAndStartDownload() = mutexLock.withLock {
    if (jmmHistoryMetadata.taskId == null ||
      (jmmHistoryMetadata.state.state != JmmStatus.INSTALLED &&
          jmmHistoryMetadata.state.state != JmmStatus.Completed)
    ) {
      jmmController.createDownloadTask(jmmHistoryMetadata)
    }
    // 已经注册完监听了，开始
    startDownload()
  }

  suspend fun startDownload() {
    jmmController.start(jmmHistoryMetadata).falseAlso {
      showToastText(BrowserI18nResource.toast_message_download_download_fail.text)
      jmmHistoryMetadata.updateState(JmmStatus.Failed)
    }
  }

  suspend fun pauseDownload() = jmmController.pause(jmmHistoryMetadata.taskId)

  suspend fun cancel() = jmmController.cancel(jmmHistoryMetadata.taskId)

  suspend fun exists() = jmmController.exists(jmmHistoryMetadata.taskId)

  suspend fun closeSelf() {
    jmmNMM.getOrOpenMainWindow().closeRoot()
  }

  suspend fun showToastText(message: String) = jmmController.showToastText(message)
}