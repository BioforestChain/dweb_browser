package org.dweb_browser.browser.jmm

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore

@Serializable
data class JsMicroModuleDBItem(val installManifest: JmmAppInstallManifest, val originUrl: String)

class JmmStore(microModule: MicroModule) {
  private val store = microModule.createStore("JmmApps", false)

  suspend fun getOrPut(key: MMID, value: JsMicroModuleDBItem): JsMicroModuleDBItem {
    return store.getOrPut(key) { value }
  }

  suspend fun get(key: MMID): JsMicroModuleDBItem? {
    return store.getOrNull(key)
  }

  suspend fun getAll(): MutableMap<MMID, JsMicroModuleDBItem> {
    return store.getAll()
  }

  suspend fun set(key: MMID, value: JsMicroModuleDBItem) {
    store.set(key, value)
  }

  suspend fun delete(key: MMID): Boolean {
    return store.delete(key)
  }
}
