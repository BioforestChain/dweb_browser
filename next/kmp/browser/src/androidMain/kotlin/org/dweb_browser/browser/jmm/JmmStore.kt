package org.dweb_browser.browser.jmm

import kotlinx.serialization.Serializable
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore

@Serializable
data class JsMicroModuleDBItem(val installManifest: JmmAppInstallManifest, val originUrl: String)

class JmmStore(microModule: MicroModule) {
  private val storeApp = microModule.createStore("JmmApps", false)
  private val storeTaskId = microModule.createStore("DownloadTaskId", false)
  private val storeMetadataUrl = microModule.createStore("MetadataUrl", false)

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
  suspend fun saveMetadata(url: String, metadata: JmmAppInstallManifest) {
    storeMetadataUrl.set(url, metadata)
  }

  suspend fun getAllMetadata(): MutableMap<String, JmmAppInstallManifest> {
    return storeMetadataUrl.getAll()
  }

  suspend fun getMetadata(url: String): String? {
    return storeMetadataUrl.getOrNull<String>(url)
  }

  suspend fun deleteMetadata(url: String): Boolean {
    return storeMetadataUrl.delete(url)
  }
}
