package org.dweb_browser.browser.download

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore

class DownloadStore(mm:MicroModule) {
  private val store = mm.createStore("downloadTask", false)

  suspend fun getOrPut(key: String,value:DownloadTask):DownloadTask {
    return store.getOrPut(key) {value}
  }

  suspend fun get(key: String):DownloadTask {
    return store.get(key)
  }

  suspend fun getAll():MutableMap<String, DownloadTask> {
    return store.getAll()
  }

  suspend fun set(key:String, value:DownloadTask) {
    store.set(key,value)
  }

}