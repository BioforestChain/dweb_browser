package org.dweb_browser.browser.jmm

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.ImageResource

class JmmStore(microModule: MicroModule) {
  private val store = microModule.createStore("JmmApps", false)

  suspend fun getOrPut(key: String, value: DeskAppInfo): DeskAppInfo {
    return store.getOrPut(key) { value }
  }

  suspend fun get(key: String): DeskAppInfo {
    return store.get(key)
  }

  suspend fun getAll(): MutableMap<String, DeskAppInfo> {
    return store.getAll()
  }

  suspend fun set(key: String, value: DeskAppInfo) {
    store.set(key, value)
  }
  suspend fun delete(key: String): Boolean {
   return store.delete(key)
  }
}

enum class AppType(val type: String) {
  Jmm("jmm"),
  Link("link"),
}

@Serializable
data class DeskWebLink(
  val id: String,
  val title: String,
  val url: String,
  val icon: ImageResource
)

@Serializable
data class DeskAppInfo(
  val appType: AppType,
  val metadata: JmmAppInstallManifest? = null,
  val weblink: DeskWebLink? = null,
)

