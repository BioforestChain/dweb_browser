package org.dweb_browser.browser.jmm

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.download.model.DownloadState
import org.dweb_browser.browser.download.model.DownloadStateEvent
import org.dweb_browser.browser.download.model.DownloadTask
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.datetimeNow

@Serializable
data class JsMicroModuleDBItem(
  val installManifest: JmmAppInstallManifest,
  val originUrl: String,
  val referrerUrl: String? = null,
) {
  val jmmMetadata by lazy {
    installManifest.createJmmMetadata(originUrl, referrerUrl, JmmStatus.INSTALLED)
  }
}

class JmmStore(microModule: MicroModule.Runtime) {
  private val storeApp = microModule.createStore("jmm_apps", false)
  private val storeHistory = microModule.createStore("history_metadata", false)

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
  suspend fun saveHistory(mmid: String, metadata: JmmMetadata) {
    storeHistory.set(mmid, metadata)
  }

  suspend fun getAllHistory(): Map<String, JmmMetadata> {
    return storeHistory.getAll()
  }

  suspend fun getHistory(mmid: String): String? {
    return storeHistory.getOrNull<String>(mmid)
  }

  suspend fun deleteHistory(mmid: String): Boolean {
    return storeHistory.delete(mmid)
  }

  suspend fun clearHistory() = storeHistory.clear()
}

/**
 * 用于存储安装历史记录
 */
@Serializable
data class JmmMetadata(
  val originUrl: String,
  val referrerUrl: String? = null,
  @SerialName("metadata") @Deprecated("use manifest alternative") private val _oldMetadata: JmmAppInstallManifest? = null,
  /**
   * 目前兼容模式，使用 Nullable，未来这个字段不可空
   */
  @SerialName("manifest") private var _manifest: JmmAppInstallManifest? = null,
  var downloadTask: DownloadTask? = null, // 用于保存下载任务，下载完成置空
  @SerialName("state") private var _state: JmmStatusEvent = JmmStatusEvent(), // 用于显示下载状态
  var installTime: Long = datetimeNow(), // 表示安装应用的时间
  var upgradeTime: Long = datetimeNow(),
) {
  init {
    @Suppress("DEPRECATION") if (_oldMetadata != null) {
      _manifest = _oldMetadata
    }
  }

  var state by ObservableMutableState(_state) { _state = it }
  var manifest by ObservableMutableState(_manifest!!) { _manifest = it }
  suspend fun initDownloadTask(downloadTask: DownloadTask, store: JmmStore) {
    this.downloadTask = downloadTask
    updateDownloadStatus(downloadTask.status, store)
  }

  suspend fun updateDownloadStatus(
    status: DownloadStateEvent,
    store: JmmStore,
    saveMetadata: Boolean = true,
  ) {
    val newStatus = JmmStatusEvent(
      current = status.current, total = status.total, state = when (status.state) {
        DownloadState.Init -> JmmStatus.Init
        DownloadState.Downloading -> JmmStatus.Downloading
        DownloadState.Paused -> JmmStatus.Paused
        DownloadState.Failed -> JmmStatus.Failed
        DownloadState.Canceled -> JmmStatus.Canceled
        DownloadState.Completed -> JmmStatus.Completed
      }
    )
    if (newStatus != state) { // 只要前后不一样，就进行保存，否则不保存，主要为了防止downloading频繁保存
      state = newStatus
      if (saveMetadata) {
        store.saveHistory(this.manifest.id, this@JmmMetadata)
      }
    }
  }

  suspend fun initState(store: JmmStore) {
    state = state.copy(state = JmmStatus.Init)
    store.saveHistory(this.manifest.id, this@JmmMetadata)
  }

  suspend fun installComplete(store: JmmStore) {
    debugJMM("installComplete")
    state = state.copy(state = JmmStatus.INSTALLED)
    store.saveHistory(this.manifest.id, this)
    store.setApp(manifest.id, JsMicroModuleDBItem(manifest, originUrl, referrerUrl))
  }

  suspend fun installFail(store: JmmStore) {
    debugJMM("installFail")
    state = state.copy(state = JmmStatus.Failed)
    store.saveHistory(this.manifest.id, this)
  }
}

@Serializable
data class JmmStatusEvent(
  val current: Long = 0,
  val total: Long = 1,
  val state: JmmStatus = JmmStatus.Init,
) {
  val progress by lazy {
    when (total) {
      0L -> .0f
      else -> current.toFloat() / total.toFloat()
    }
  }
}

fun JmmAppInstallManifest.createJmmMetadata(
  originUrl: String,
  referrerUrl: String?,
  state: JmmStatus,
  installTime: Long = datetimeNow(),

  ) = JmmMetadata(
  originUrl = originUrl,
  referrerUrl = referrerUrl,
  _manifest = this,
  _state = JmmStatusEvent(total = this.bundle_size, state = state),
  installTime = installTime
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
  NewVersion,

  /** 版本偏低 */
  VersionLow;
}

enum class JmmTabs(
  val index: Int,
  val title: SimpleI18nResource,
  val vector: ImageVector,
  val listFilter: (Iterable<JmmMetadata>) -> List<JmmMetadata>,
) {
  Installed(
    index = 1,
    title = JmmI18nResource.history_tab_installed,
    vector = Icons.Default.InstallMobile,
    listFilter = { list ->
      list.filter { it.state.state == JmmStatus.INSTALLED }.sortedBy { it.installTime }
    },
  ),
  NoInstall(
    index = 0,
    title = JmmI18nResource.history_tab_uninstalled,
    vector = Icons.Default.DeleteForever,
    listFilter = { list ->
      list.filter { it.state.state != JmmStatus.INSTALLED }.sortedBy { it.upgradeTime }
    },
  ),
  ;
}