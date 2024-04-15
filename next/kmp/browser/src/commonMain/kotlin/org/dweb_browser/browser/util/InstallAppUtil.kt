package org.dweb_browser.browser.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.datetimeNow

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
}

expect class InstallAppUtil() {
  suspend fun loadNewVersion(): NewVersionItem?

  fun openSystemInstallSetting() // 打开系统的授权安装界面
  fun installApp(realPath: String): Boolean // 安装应用

  fun openOrShareFile(realPath: String) // 直接打开或者执行分享操作
}