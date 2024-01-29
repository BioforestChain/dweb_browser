package org.dweb_browser.browser.download

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore

typealias TaskId = String

class DownloadStore(mm: MicroModule) {
  private val store = mm.createStore("download_task", false)

  suspend fun getOrPut(key: TaskId, value: DownloadTask): DownloadTask {
    return store.getOrPut(key) { value }
  }

  suspend fun get(key: TaskId): DownloadTask? {
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
}