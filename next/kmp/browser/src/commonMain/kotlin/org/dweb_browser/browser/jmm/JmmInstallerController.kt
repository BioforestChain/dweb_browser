package org.dweb_browser.browser.jmm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.jmm.ui.Render
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.sys.window.core.modal.WindowBottomSheetsController
import org.dweb_browser.sys.window.ext.createBottomSheets
import org.dweb_browser.sys.window.ext.getMainWindowId
import org.dweb_browser.sys.window.ext.getOrOpenMainWindow
import org.dweb_browser.sys.window.ext.getWindow

internal val LocalShowWebViewVersion = compositionChainOf("ShowWebViewVersion") {
  mutableStateOf(false)
}

internal val LocalJmmInstallerController =
  compositionChainOf<JmmInstallerController>("JmmInstallerController")

/**
 * JS 模块安装 的 控制器
 */
class JmmInstallerController(
  metadata: JmmMetadata,
  internal val jmmNMM: JmmNMM.JmmRuntime,
  private val jmmController: JmmController,
) {
  var installMetadata by ObservableMutableState(metadata) {}
    internal set

  private var viewDeferred = CompletableDeferred<WindowBottomSheetsController>()
  private val getViewLock = Mutex()

  @OptIn(ExperimentalCoroutinesApi::class)
  suspend fun getBottomSheetView() = getViewLock.withLock {
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
      jmmNMM.getWindow(it.wid).hide()
      it.onDestroy {
        viewDeferred = CompletableDeferred()
      }
    }
  }

  suspend fun openRender() {
    /// 显示抽屉
    val bottomSheets = getBottomSheetView()
    bottomSheets.open()
  }

  /**安装完成后打开app*/
  suspend fun openApp() {
    closeSelf() // 打开应用之前，需要关闭当前安装界面，否则在原生窗口的层级切换会出现问题
    jmmNMM.bootstrapContext.dns.open(installMetadata.metadata.id)
  }

  // 关闭原来的app
  suspend fun closeApp() {
    jmmNMM.bootstrapContext.dns.close(installMetadata.metadata.id)
  }

  /**
   * 创建任务，如果存在则恢复
   */
  suspend fun createAndStartDownload() {
    jmmController.startDownloadTask(installMetadata)
  }

  suspend fun startDownload() = jmmController.startDownloadTask(installMetadata)

  suspend fun pause() = jmmController.pause(installMetadata)

  suspend fun closeSelf() {
    jmmNMM.getOrOpenMainWindow().closeRoot()
  }
}