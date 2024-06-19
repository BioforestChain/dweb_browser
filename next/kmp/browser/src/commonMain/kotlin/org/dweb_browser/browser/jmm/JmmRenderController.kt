package org.dweb_browser.browser.jmm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.dweb_browser.browser.jmm.ui.Render
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow

/**
 * JS 模块安装 的 控制器
 */
class JmmRenderController(
  internal val jmmNMM: JmmNMM.JmmRuntime, private val jmmController: JmmController,
) {
  fun getHistoryMetadataMap() = jmmController.historyMetadataMaps

  suspend fun hideView() {
    jmmNMM.getMainWindow().hide()
  }

  /**打开jmm下载历史视图*/
  suspend fun showView(win: WindowController) {
    windowAdapterManager.provideRender(win.id) { modifier ->
      Render(modifier = modifier, windowRenderScope = this)
    }
    win.show()
  }

  suspend fun buttonClick(historyMetadata: JmmMetadata) {
    when (historyMetadata.state.state) {
      JmmStatus.INSTALLED -> {
        // jmmNMM.bootstrapContext.dns.open(historyMetadata.metadata.id)
        jmmController.openApp(historyMetadata.manifest.id)
      }

      JmmStatus.Paused -> {
        jmmController.startDownloadTask(historyMetadata)
      }

      JmmStatus.Downloading -> {
        jmmController.pause(historyMetadata)
      }

      JmmStatus.Completed -> {}
      else -> {
        jmmController.startDownloadTaskByUrl(historyMetadata.originUrl, historyMetadata.referrerUrl)
      }
    }
  }

  var detailController by mutableStateOf<JmmInstallerController?>(null)
    private set

  /** 打开详情界面*/
  fun openDetail(historyMetadata: JmmMetadata) {
    detailController = jmmController.getInstallerController(historyMetadata)
  }

  fun closeDetail() {
    detailController = null
  }

  /// 卸载app
  fun unInstall(historyMetadata: JmmMetadata) {
    jmmNMM.scopeLaunch(cancelable = false) {
      jmmController.uninstall(historyMetadata.manifest.id)
    }
  }

  fun removeHistoryMetadata(historyMetadata: JmmMetadata) {
    jmmNMM.scopeLaunch(cancelable = false) {
      jmmController.removeHistoryMetadata(historyMetadata)
    }
  }
}