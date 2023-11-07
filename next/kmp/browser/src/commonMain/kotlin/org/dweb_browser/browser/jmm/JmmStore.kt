package org.dweb_browser.browser.jmm

import kotlinx.serialization.Serializable
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.browser.jmm.model.JmmStatus
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.isGreaterThan

@Serializable
data class JsMicroModuleDBItem(val installManifest: JmmAppInstallManifest, val originUrl: String)

class JmmStore(microModule: MicroModule) {
  private val storeApp = microModule.createStore("JmmApps", false)
  private val storeTaskId = microModule.createStore("DownloadTaskId", false)
  private val storeHistoryMetadata = microModule.createStore("MetadataUrl", false)

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
   * 下载相关的函数
   */
  suspend fun saveJMMTaskId(mmid: MMID, taskId: TaskId) {
    storeTaskId.set(mmid, taskId)
  }

  suspend fun getAllJMMTaskId(): MutableMap<MMID, String> {
    return storeTaskId.getAll()
  }

  suspend fun getTaskId(mmid: MMID): String? {
    return storeTaskId.getOrNull<String>(mmid)
  }

  suspend fun deleteJMMTaskId(mmid: MMID): Boolean {
    return storeTaskId.delete(mmid)
  }

  /*****************************************************************************
   * JMM对应的json地址存储
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
  var taskId: TaskId? = null, // 用于保存下载任务，下载完成置空。
  var state: JmmStatus = JmmStatus.Init, // 用于显示下载状态。
  var installTime: Long = datetimeNow(), // 表示安装应用的时间
) {
  suspend fun initTaskId(taskId: TaskId, store: JmmStore) {
    this.taskId = taskId
    store.saveHistoryMetadata(originUrl, this)
  }

  suspend fun downloadComplete(store: JmmStore) {
    taskId = null
    state = JmmStatus.Completed
    store.saveHistoryMetadata(originUrl, this)
  }

  suspend fun installFail(store: JmmStore) {
    state = JmmStatus.Failed
    store.saveHistoryMetadata(originUrl, this)
  }

  suspend fun newVersion(store: JmmStore, newMetadata: JmmAppInstallManifest): JmmHistoryMetadata {
    if (newMetadata.version.isGreaterThan(this.metadata.version)) {
      state = JmmStatus.NewVersion
      store.saveHistoryMetadata(originUrl, this)
    }
    return this
  }

  suspend fun installComplete(store: JmmStore) {
    taskId = null
    state = JmmStatus.INSTALLED
    installTime = datetimeNow()
    store.saveHistoryMetadata(originUrl, this)
  }
}

fun JmmAppInstallManifest.createJmmHistoryMetadata(url: String) = JmmHistoryMetadata(
  originUrl = url,
  metadata = this,
)