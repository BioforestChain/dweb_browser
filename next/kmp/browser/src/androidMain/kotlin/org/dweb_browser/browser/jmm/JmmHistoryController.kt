package org.dweb_browser.browser.jmm

import org.dweb_browser.browser.jmm.ui.ManagerViewRender
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow

/**
 * JS 模块安装 的 控制器
 */
class JmmHistoryController(private val jmmNMM: JmmNMM) {

  val jmmMetadataList = jmmNMM.jmmMetadataList

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

  suspend fun close() { jmmNMM.getMainWindow().hide() }
}