package org.dweb_browser.dwebview

interface ICloseWatcher {
  interface IWatcher {
    val id: String
    suspend fun tryClose(): Boolean
    fun destroy(): Boolean
  }

  val canClose: Boolean
  suspend fun close(watcher: IWatcher): Boolean
  suspend fun close(): Boolean
}