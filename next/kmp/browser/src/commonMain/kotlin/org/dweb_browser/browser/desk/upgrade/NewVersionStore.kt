package org.dweb_browser.browser.desk.upgrade

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadStateEvent
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.datetimeNow

@Serializable
class NewVersionItem(
  val originUrl: String,
  val versionName: String,
) {
  val versionCode: Long = datetimeNow() // 暂时不确定是否用到，先申明一个预留字段
  val description: String? = null  //表示升级内容描述

  @SerialName("status")
  private var _status: NewVersionStatus = NewVersionStatus()
  val forceUpdate: Boolean = false  // 是否强制更新
  var taskId: TaskId? = null

  @Serializable
  data class NewVersionStatus(
    val current: Long = 0,
    val total: Long = 1,
    val state: DownloadState = DownloadState.Init,
  )

  var status by ObservableMutableState(_status) { _status = it }

  suspend fun initDownloadTask(downloadTask: DownloadTask, store: NewVersionStore) {
    this.taskId = downloadTask.id
    this.updateDownloadStatus(downloadTask.status, store)
  }

  suspend fun updateDownloadStatus(status: DownloadStateEvent, store: NewVersionStore) {
    val newStatus =
      NewVersionStatus(current = status.current, total = status.total, state = status.state)
    if (newStatus != this.status) {
      this.status = newStatus
      store.setNewVersion(this)
    }
  }

  fun progress(): Float {
    return if (_status.total == 0L) {
      .0f
    } else {
      (_status.current * 1.0f / _status.total) * 10 / 10.0f
    }
  }
}

class NewVersionStore(mm: MicroModule.Runtime) {
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