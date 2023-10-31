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
  suspend fun saveJMMTask(mmid: MMID, taskId: TaskId) {
    storeTaskId.set(mmid, taskId)
  }

  suspend fun getAllJMMTask(): MutableMap<MMID, String> {
    return storeTaskId.getAll()
  }

  suspend fun getTaskId(mmid: MMID): String? {
    return storeTaskId.getOrNull<String>(mmid)
  }

  suspend fun deleteJMMTask(mmid: MMID): Boolean {
    return storeTaskId.delete(mmid)
  }
}
