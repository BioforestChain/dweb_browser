package org.dweb_browser.browser.jmm

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import org.dweb_browser.browser.jmm.model.JmmStatus
import org.dweb_browser.browser.jmm.ui.ManagerViewRender
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow

/**
 * JS 模块安装 的 控制器
 */
class JmmHistoryController(
  private val jmmNMM: JmmNMM, private val store: JmmStore, jmmController: JmmController
) {
  val jmmHistoryMetadata = mutableStateListOf<JmmHistoryMetadata>()
  private val jmmHistoryMap = mutableStateMapOf<String, JmmHistoryMetadata>()

  fun getTaskId(originUrl: String) = jmmHistoryMap[originUrl]?.taskId


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

  /**
   * 获取历史下载任务
   */
  suspend fun loadHistoryMetadataUrl() {
    this.jmmHistoryMap.putAll(store.getHistoryMetadata()) // 保存所有数据
    this.jmmHistoryMetadata.addAll(store.getHistoryMetadata().values.sortedBy { it.installTime })
    // 这边需要增加创建任务的操作
    this.jmmHistoryMetadata.forEach { item ->
      if (item.state == JmmStatus.Downloading) {
        item.state = JmmStatus.Paused
      }
    }
  }

  suspend fun createNewMetadata(
    originUrl: String, jmmAppInstallManifest: JmmAppInstallManifest
  ) : JmmHistoryMetadata {
    val newMetadata = jmmAppInstallManifest.createJmmHistoryMetadata(originUrl).apply {
      jmmHistoryMetadata.add(0, this)
    }
    store.saveHistoryMetadata(originUrl, newMetadata)
    return newMetadata
  }
}