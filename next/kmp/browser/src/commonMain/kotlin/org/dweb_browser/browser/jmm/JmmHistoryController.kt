package org.dweb_browser.browser.jmm

import org.dweb_browser.browser.jmm.ui.ManagerViewRender
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow

/**
 * JS 模块安装 的 控制器
 */
class JmmHistoryController(
  internal val jmmNMM: JmmNMM.JmmRuntime, private val jmmController: JmmController
) {
  fun getHistoryMetadataMap() = jmmController.historyMetadataMaps

  suspend fun hideView() {
    jmmNMM.getMainWindow().hide()
  }

  /**打开jmm下载历史视图*/
  suspend fun showHistoryView(win: WindowController) {
    windowAdapterManager.provideRender(win.id) { modifier ->
      ManagerViewRender(modifier = modifier, windowRenderScope = this)
    }
    win.open()
  }

  suspend fun buttonClick(historyMetadata: JmmMetadata) {
    when (historyMetadata.state.state) {
      JmmStatus.INSTALLED -> {
        jmmNMM.bootstrapContext.dns.open(historyMetadata.manifest.id)
      }

      JmmStatus.Paused -> {
        jmmController.startDownloadTask(historyMetadata)
      }

      JmmStatus.Downloading -> {
        jmmController.pause(historyMetadata)
      }

      JmmStatus.Completed -> {}
      else -> {
        jmmController.startDownloadTask(historyMetadata)
      }
    }
  }

  /** 打开详情界面*/
  fun openDetail(historyMetadata: JmmMetadata) = jmmNMM.scopeLaunch(cancelable = false) {
    jmmController.openBottomSheet(historyMetadata)
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