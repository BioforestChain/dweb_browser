package org.dweb_browser.browser.web.model

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.digest.SHA256
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.BitmapUtil
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.toUtf8ByteArray

private const val DESK_WEBLINK_ICONS = "file:///web_icons/"

@Serializable
data class WebLinkManifest(
  val id: String, val title: String, val url: String, val icons: List<ImageResource>
) {
  companion object {
    private val sha256 = CryptographyProvider.Default.get(SHA256)

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun createLinkId(url: String) = "${
      sha256.hasher().hash(url.toUtf8ByteArray()).toHexString(0, 4, HexFormat.Default)
    }.link.dweb"

    fun bitmapToImageResource(context: Context, bitmap: ImageBitmap): ImageResource? {
      return BitmapUtil.saveBitmapToIcons(context, bitmap.asAndroidBitmap())?.let { src ->
        ImageResource(src = "$DESK_WEBLINK_ICONS$src")
      }
    }
  }
}

class WebLinkStore(microModule: MicroModule) {
  private val store = microModule.createStore("WebLinkApps", false)

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