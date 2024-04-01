package org.dweb_browser.browser.jmm

import kotlinx.coroutines.launch
import org.dweb_browser.browser.jmm.ui.ManagerViewRender
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow

/**
 * JS 模块安装 的 控制器
 */
class JmmHistoryController(
  internal val jmmNMM: JmmNMM, private val jmmController: JmmController
) {
  fun getHistoryMetadataMap() = jmmController.historyMetadataMaps

  suspend fun close() {
    jmmNMM.getMainWindow().hide()
  }

  suspend fun openHistoryView(win: WindowController) {
    // 主界面定义
    win.unMaximize()
    windowAdapterManager.provideRender(win.id) { modifier ->
      ManagerViewRender(modifier = modifier, windowRenderScope = this)
    }
    win.show()
  }

  suspend fun buttonClick(historyMetadata: JmmHistoryMetadata) {
    when (historyMetadata.state.state) {
      JmmStatus.INSTALLED -> {
        jmmNMM.nativeFetch("file://desk.browser.dweb/openAppOrActivate?app_id=${historyMetadata.metadata.id}")
      }

      JmmStatus.Paused -> {
        jmmController.startDownloadTask(historyMetadata)
      }

      JmmStatus.Downloading -> {
        jmmController.pause(historyMetadata)
      }

      JmmStatus.Completed -> {}
      else -> {
        jmmController.createDownloadTask(historyMetadata)
        jmmController.startDownloadTask(historyMetadata)
      }
    }
  }

  fun openInstallerView(historyMetadata: JmmHistoryMetadata) = jmmNMM.mmScope.launch {
    jmmController.openOrUpsetInstallerView(historyMetadata.originUrl, historyMetadata, true)
  }

  fun unInstall(historyMetadata: JmmHistoryMetadata) {
    jmmNMM.mmScope.launch {
      jmmController.uninstall(historyMetadata.metadata.id)
    }
  }

  fun removeHistoryMetadata(historyMetadata: JmmHistoryMetadata) {
    jmmNMM.mmScope.launch {
      jmmController.removeHistoryMetadata(historyMetadata)
    }
  }
}