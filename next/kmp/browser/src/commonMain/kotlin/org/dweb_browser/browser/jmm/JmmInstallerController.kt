package org.dweb_browser.browser.jmm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.jmm.ui.Render
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.compose.compositionChainOf
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
  var installMetadata by ObservableMutableState(initJmmHistoryMetadata) {}
    internal set

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
    jmmNMM.createBottomSheets { modifier ->
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
    }
  }

  suspend fun openApp() {
    jmmNMM.nativeFetch("file://desk.browser.dweb/openAppOrActivate?app_id=${installMetadata.metadata.id}")
    closeSelf() // 打开应用后，需要关闭当前安装界面
  }

  // 关闭原来的app
  suspend fun closeApp() {
    jmmNMM.nativeFetch("file://desk.browser.dweb/closeApp?app_id=${installMetadata.metadata.id}")
  }

  /**
   * 创建任务，如果存在则恢复
   */
  suspend fun createAndStartDownload() = jmmController.createAndStartDownloadTask(installMetadata)

  suspend fun startDownload() = jmmController.startDownloadTask(installMetadata)

  suspend fun pause() = jmmController.pause(installMetadata.taskId)

  suspend fun cancel() = jmmController.cancel(installMetadata.taskId)

  suspend fun exists() = jmmController.exists(installMetadata.taskId)

  suspend fun closeSelf() { jmmNMM.getOrOpenMainWindow().closeRoot() }
}