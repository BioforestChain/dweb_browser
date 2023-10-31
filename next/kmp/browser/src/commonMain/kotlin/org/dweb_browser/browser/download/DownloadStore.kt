package org.dweb_browser.browser.download

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore

typealias TaskId = String

class DownloadStore(mm: MicroModule) {
  private val store = mm.createStore("downloadTask", false)
  private val storeComplete = mm.createStore("downloadComplete", false)

  suspend fun getOrPut(key: TaskId, value: DownloadTask): DownloadTask {
    return store.getOrPut(key) { value }
  }

  suspend fun get(key: String): DownloadTask? {
    return store.getOrNull(key)
  }

  suspend fun getAll(): MutableMap<TaskId, DownloadTask> {
    return store.getAll()
  }

  suspend fun set(key: TaskId, value: DownloadTask) {
    store.set(key, value)
  }

  suspend fun delete(key: TaskId) {
    store.delete(key)
  }

  /************************************************************
   * 下面是下载结束的保存，包含了下载失败，和下载成功等
   */
  suspend fun setComplete(key: TaskId, value: DownloadTask) = storeComplete.set(key, value)
  suspend fun deleteComplete(key: TaskId) = storeComplete.delete(key)
  suspend fun getAllCompletes(): MutableMap<TaskId, DownloadTask> = storeComplete.getAll()
}