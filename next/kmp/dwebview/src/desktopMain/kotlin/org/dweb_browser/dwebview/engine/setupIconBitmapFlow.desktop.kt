package org.dweb_browser.dwebview.engine

import androidx.compose.ui.graphics.ImageBitmap
import com.teamdev.jxbrowser.browser.event.FaviconChanged
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.defaultHttpPureClient

class CacheImageBitmap(val browserUrl: String) {
  val bitmaps = mutableMapOf<String, ImageBitmap?>()

  companion object {
    private var default = CacheImageBitmap("")
    fun getOrPut(browserUrl: String): MutableMap<String, ImageBitmap?> {
      if (default.browserUrl != browserUrl) {
        default = CacheImageBitmap(browserUrl)
      }
      return default.bitmaps
    }

    fun pickImageBitmap() =
      default.bitmaps.values.filterNotNull().maxByOrNull { it.width * it.height }
  }
}

fun setupIconBitmapFlow(engine: DWebViewEngine) =
  MutableStateFlow<ImageBitmap?>(null).also { stateFlow ->
    /// 手动下载图标，高优先级。因为这是高分辨率的
    engine.dwebFavicon.urlFlow.collectIn(engine.lifecycleScope) { faviconHref ->
      engine.getOriginalUrl().also { browserUrl ->
        CacheImageBitmap.getOrPut(browserUrl)["Height"] = if (faviconHref.isNotEmpty()) {
          runCatching {
            defaultHttpPureClient.fetch(PureClientRequest(faviconHref, method = PureMethod.GET))
              .binary().toImageBitmap()
          }.getOrNull()
        } else null

        stateFlow.value = CacheImageBitmap.pickImageBitmap()
      }
    }
    /// 原生地，获得图标，低优先级。因为这默认是低分辨率的
    engine.browser.on(FaviconChanged::class.java) { event ->
      val browserUrl = event.browser().url()
      CacheImageBitmap.getOrPut(browserUrl)["Low"] = event.favicon().let { favicon ->
        if (favicon.size().isEmpty) null else favicon.toImageBitmap()
      }
      stateFlow.value = CacheImageBitmap.pickImageBitmap()
    }
  }
