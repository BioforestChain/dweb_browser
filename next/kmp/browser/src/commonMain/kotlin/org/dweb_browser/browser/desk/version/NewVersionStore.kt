package org.dweb_browser.browser.desk.version

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.datetimeNow

expect class NewVersionManage() {
  suspend fun loadNewVersion(): NewVersionItem?

  fun openSystemInstallSetting() // 打开系统的授权安装界面
  fun installApk(realPath: String)
}

@Serializable
data class NewVersionItem(
  val originUrl: String,
  val versionCode: Long = datetimeNow(), // 暂时不确定是否用到，先申明一个预留字段
  val versionName: String,
  val description: String? = null, //表示升级内容描述
  var taskId: TaskId? = null,
  @SerialName("status")
  private var _status: NewVersionStatus = NewVersionStatus(),
  val forceUpdate: Boolean = false, // 是否强制更新
) {
  @Serializable
  data class NewVersionStatus(
    val current: Long = 0,
    val total: Long = 1,
    val state: DownloadState = DownloadState.Init,
  )

  var status by ObservableMutableState(_status) { _status = it }

  @Transient
  var alreadyWatch: Boolean = false

  suspend fun updateTaskId(taskId: TaskId, store: NewVersionStore) {
    this.taskId = taskId
    store.setNewVersion(this)
  }

  suspend fun updateDownloadTask(downloadTask: DownloadTask, store: NewVersionStore) {
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