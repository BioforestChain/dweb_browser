package org.dweb_browser.browser.web.data

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.crypto.hash.sha256

@Serializable
data class WebLinkManifest(
  val id: String, val title: String, val url: String, val icons: List<ImageResource>
) {
  companion object {

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun createLinkId(url: String) = "${
      sha256(url).toHexString(0, 4, HexFormat.Default)
    }.link.dweb"
  }
}

class WebLinkStore(microModule: MicroModule.Runtime) {
  private val store = microModule.createStore("web_link_apps", false)

  suspend fun getOrPut(key: MMID, value: WebLinkManifest): WebLinkManifest {
    return store.getOrPut(key) { value }
  }

  suspend fun get(key: MMID): WebLinkManifest? {
    return store.getOrNull(key)
  }

  suspend fun getAll(): MutableMap<MMID, WebLinkManifest> {
    return store.getAll()
  }

  suspend fun set(key: MMID, value: WebLinkManifest) {
    store.set(key, value)
  }

  suspend fun delete(key: MMID): Boolean {
    return store.delete(key)
  }
}