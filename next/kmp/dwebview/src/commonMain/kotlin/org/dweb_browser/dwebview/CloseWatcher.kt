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
}