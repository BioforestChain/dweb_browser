package org.dweb_browser.browser.desk.upgrade

import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.browser.util.NewVersionItem
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore

suspend fun NewVersionItem.updateTaskId(taskId: TaskId, store: NewVersionStore) {
  this.taskId = taskId
  store.setNewVersion(this)
}

suspend fun NewVersionItem.updateDownloadTask(downloadTask: DownloadTask, store: NewVersionStore) {
  val lastState = this.status.state
  this.status = this.status.copy(
    current = downloadTask.status.current,
    total = downloadTask.status.total,
    state = downloadTask.status.state
  )
  if (lastState != this.status.state) {
    store.setNewVersion(this)
  }
}

class NewVersionStore(mm: MicroModule) {
  private val store = mm.createStore("NewVersion", false)
  private val newVersionKey = "NewVersionKey"

  suspend fun getNewVersion() = store.getOrNull<NewVersionItem>(newVersionKey)

  suspend fun setNewVersion(data: NewVersionItem) {
    return store.set(newVersionKey, data)
  }

  suspend fun clear() {
    return store.clear()
  }
}