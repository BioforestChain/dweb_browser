package org.dweb_browser.dwebview

import kotlinx.coroutines.flow.StateFlow

interface ICloseWatcher {
  interface IWatcher {
    val id: String
    suspend fun tryClose(): Boolean
    fun destroy(): Boolean
  }

  val canClose: Boolean
  val canCloseFlow: StateFlow<Boolean>
  suspend fun close(watcher: IWatcher): Boolean
  suspend fun close(): Boolean

  /**
   * 重置所有状态，通常在页面文档变更时是调用
   * 比如前进到新的页面，后退到旧页面
   */
  fun reset()
}