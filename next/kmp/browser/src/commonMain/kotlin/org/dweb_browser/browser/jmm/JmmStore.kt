package org.dweb_browser.browser.jmm

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.datetimeNow

@Serializable
data class JsMicroModuleDBItem(val installManifest: JmmAppInstallManifest, val originUrl: String)

class JmmStore(microModule: MicroModule) {
  private val storeApp = microModule.createStore("jmm_apps", false)
  private val storeHistoryMetadata = microModule.createStore("history_metadata", false)

  suspend fun getOrPutApp(key: MMID, value: JsMicroModuleDBItem): JsMicroModuleDBItem {
    return storeApp.getOrPut(key) { value }
  }

  suspend fun getApp(key: MMID): JsMicroModuleDBItem? {
    return storeApp.getOrNull(key)
  }

  suspend fun getAllApps(): MutableMap<MMID, JsMicroModuleDBItem> {
    return storeApp.getAll()
  }

  suspend fun setApp(key: MMID, value: JsMicroModuleDBItem) {
    storeApp.set(key, value)
  }

  suspend fun deleteApp(key: MMID): Boolean {
    return storeApp.delete(key)
  }

  /*****************************************************************************
   * JMM对应的json地址存储，以及下载的 taskId 信息
   */
  suspend fun saveHistoryMetadata(url: String, metadata: JmmHistoryMetadata) {
    storeHistoryMetadata.set(url, metadata)
  }

  suspend fun getHistoryMetadata(): MutableMap<String, JmmHistoryMetadata> {
    return storeHistoryMetadata.getAll()
  }

  suspend fun getHistoryMetadata(url: String): String? {
    return storeHistoryMetadata.getOrNull<String>(url)
  }

  suspend fun deleteHistoryMetadata(url: String): Boolean {
    return storeHistoryMetadata.delete(url)
  }
}

/**
 * 用于存储安装历史记录
 */
@Serializable
data class JmmHistoryMetadata(
  val originUrl: String,
  val metadata: JmmAppInstallManifest,
  var taskId: TaskId? = null, // 用于保存下载任务，下载完成置空
  @SerialName("state")
  private var _state: JmmStatusEvent = JmmStatusEvent(), // 用于显示下载状态
  var installTime: Long = datetimeNow(), // 表示安装应用的时间
) {
  @Transient
  var state by ObservableMutableState(_state) { _state = it }

  suspend fun updateByDownloadTask(downloadTask: DownloadTask, store: JmmStore) {
    state = state.copy(
      current = downloadTask.status.current,
      total = downloadTask.status.total,
      state = when (downloadTask.status.state) {
        DownloadState.Init -> JmmStatus.Init
        DownloadState.Downloading -> JmmStatus.Downloading
        DownloadState.Paused -> JmmStatus.Paused
        DownloadState.Failed -> JmmStatus.Failed
        DownloadState.Canceled -> JmmStatus.Canceled
        DownloadState.Completed -> JmmStatus.Completed
      }
    )
    if (downloadTask.status.state != DownloadState.Downloading) {
      store.saveHistoryMetadata(originUrl, this@JmmHistoryMetadata)
    }
  }

  suspend fun updateState(jmmStatus: JmmStatus, store: JmmStore? = null) {
    state = state.copy(state = jmmStatus)
    store?.saveHistoryMetadata(originUrl, this@JmmHistoryMetadata)
  }

  suspend fun installComplete(store: JmmStore) {
    debugJMM("installComplete")
    taskId = null
    state = state.copy(state = JmmStatus.INSTALLED)
    installTime = datetimeNow()
    store.saveHistoryMetadata(originUrl, this)
    store.setApp(
      metadata.id, JsMicroModuleDBItem(metadata, originUrl)
    )
  }

  suspend fun installFail(store: JmmStore) {
    debugJMM("installFail")
    taskId = null
    state = state.copy(state = JmmStatus.Failed)
    installTime = datetimeNow()
    store.saveHistoryMetadata(originUrl, this)
  }

  // 监听下载进度 不存储到内存
  @Transient
  val jmmStatusSignal: Signal<JmmStatusEvent> = Signal()

  @Transient
  val onJmmStatusChanged = jmmStatusSignal.toListener()
}

@Serializable
data class JmmStatusEvent(
  val current: Long = 0,
  val total: Long = 1,
  val state: JmmStatus = JmmStatus.Init,
)

fun JmmAppInstallManifest.createJmmHistoryMetadata(url: String) = JmmHistoryMetadata(
  originUrl = url,
  metadata = this,
  _state = JmmStatusEvent(total = this.bundle_size)
)

@Serializable
enum class JmmStatus {
  /** 初始化中，做下载前的准备，包括寻址、创建文件、保存任务等工作 */
  Init,

  /** 下载中 */
  Downloading,

  /** 暂停下载 */
  Paused,

  /** 取消下载 */
  Canceled,

  /** 下载失败 */
  Failed,

  /** 下载完成 */
  Completed,

  /** 安装中 */
  INSTALLED,

  /** 新版本 */
  NewVersion;
}

enum class JmmTabs(val index: Int, val title: SimpleI18nResource, val vector: ImageVector) {
  NoInstall(0, BrowserI18nResource.jmm_history_tab_uninstalled, Icons.Default.DeleteForever),
  Installed(1, BrowserI18nResource.jmm_history_tab_installed, Icons.Default.InstallMobile),
  ;
}