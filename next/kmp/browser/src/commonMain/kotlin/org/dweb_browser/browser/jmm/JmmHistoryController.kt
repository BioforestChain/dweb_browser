package org.dweb_browser.browser.jmm

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import org.dweb_browser.browser.download.model.ChangeableType
import org.dweb_browser.browser.jmm.ui.ManagerViewRender
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow

/**
 * JS 模块安装 的 控制器
 */
class JmmHistoryController(
  private val jmmNMM: JmmNMM, private val jmmController: JmmController
) {
  val jmmHistoryMetadata: MutableList<JmmHistoryMetadata> = mutableStateListOf()

  init {
    jmmController.ioAsyncScope.launch {
      jmmController.historyMetadataMaps.onChange { (changeableType, _, historyMetadata) ->
        when (changeableType) {
          ChangeableType.Add -> {
            jmmHistoryMetadata.add(0, historyMetadata!!)
          }

          ChangeableType.Remove -> {
            jmmHistoryMetadata.remove(historyMetadata!!)
          }

          ChangeableType.PutAll -> {
            jmmHistoryMetadata.addAll(
              jmmController.historyMetadataMaps.toMutableList()
                .sortedByDescending { it.upgradeTime }
            )
          }

          ChangeableType.Clear -> {
            jmmHistoryMetadata.clear()
          }
        }
      }
    }
  }

  suspend fun close() {
    jmmNMM.getMainWindow().hide()
  }

  suspend fun openHistoryView(win: WindowController) {
    // 主界面定义
    win.maximize()
    windowAdapterManager.provideRender(win.id) { modifier ->
      ManagerViewRender(modifier = modifier, windowRenderScope = this)
    }
    win.show()
  }

  suspend fun openInstallerView(jmmHistoryMetadata: JmmHistoryMetadata) =
    jmmController.openOrUpsetInstallerView(
      jmmHistoryMetadata.originUrl, jmmHistoryMetadata.metadata, true
    )

  suspend fun buttonClick(historyMetadata: JmmHistoryMetadata) {
    when (historyMetadata.state.state) {
      JmmStatus.INSTALLED -> {
        jmmNMM.nativeFetch("file://desk.browser.dweb/openAppOrActivate?app_id=${historyMetadata.metadata.id}")
      }

      JmmStatus.Paused -> {
        jmmController.startDownloadTask(historyMetadata)
      }

      JmmStatus.Downloading -> {
        jmmController.pause(historyMetadata.taskId)
      }

      JmmStatus.Completed -> {}
      else -> {
        jmmController.createAndStartDownloadTask(historyMetadata)
      }
    }
  }

  suspend fun unInstall(metadata: JmmHistoryMetadata) {
    jmmController.uninstall(metadata.metadata.id)
  }

  suspend fun removeHistoryMetadata(originUrl: String) {
    jmmController.removeHistoryMetadata(originUrl)
  }
}