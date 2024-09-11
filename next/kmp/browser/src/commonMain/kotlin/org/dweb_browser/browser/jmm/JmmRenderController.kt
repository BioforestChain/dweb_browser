package org.dweb_browser.browser.jmm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
  @Composable
  fun historyMetadataMap() = jmmController.historyMetadataMapsFlow.collectAsState()

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

  var outerHistoryJmmMetadata by mutableStateOf<JmmMetadata?>(null)

  fun openDetail(historyMetadata: JmmMetadata) {
    outerHistoryJmmMetadata = historyMetadata
  }

  // 获取Jmm详情控制器，渲染详情页
  fun getJmmDetailController(historyMetadata: JmmMetadata) = jmmController.getInstallerController(historyMetadata)

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