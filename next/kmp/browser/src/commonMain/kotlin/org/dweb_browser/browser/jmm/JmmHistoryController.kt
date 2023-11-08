package org.dweb_browser.browser.jmm

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import org.dweb_browser.browser.download.model.ChangeableType
import org.dweb_browser.browser.jmm.ui.ManagerViewRender
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow

/**
 * JS 模块安装 的 控制器
 */
class JmmHistoryController(
  private val jmmNMM: JmmNMM, jmmController: JmmController
) {
  val jmmHistoryMetadata: MutableList<JmmHistoryMetadata> = mutableStateListOf()

  init {
    jmmController.ioAsyncScope.launch {
      // val list = jmmController.historyMetadataMaps.toMutableList().sortedByDescending { it.installTime }
      // jmmHistoryMetadata.addAll(list)
      jmmController.historyMetadataMaps.onChange { item ->
        when (item.first) {
          ChangeableType.Add -> {
            jmmHistoryMetadata.add(0, item.third!!)
          }

          ChangeableType.Remove -> {
            jmmHistoryMetadata.remove(item.third!!)
          }

          ChangeableType.PutAll -> {
            jmmHistoryMetadata.addAll(
              jmmController.historyMetadataMaps.toMutableList()
                .sortedByDescending { it.installTime }
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

  suspend fun openHistoryView() {
    // 主界面定义
    with(jmmNMM.getMainWindow()) {
      state.mode = org.dweb_browser.sys.window.core.constant.WindowMode.MAXIMIZE
      state.setFromManifest(jmmNMM)
      windowAdapterManager.provideRender(id) { modifier ->
        ManagerViewRender(modifier, this)
      }
      show()
    }
  }
}