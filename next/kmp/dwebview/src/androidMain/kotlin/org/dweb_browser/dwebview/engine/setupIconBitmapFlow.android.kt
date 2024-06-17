package org.dweb_browser.dwebview.engine

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.defaultHttpPureClient

fun setupIconBitmapFlow(engine: DWebViewEngine) =
  MutableStateFlow<ImageBitmap?>(null).also { stateFlow ->
    var cacheKey: String? = null
    engine.dwebFavicon.urlFlow.collectIn(engine.lifecycleScope) { faviconHref ->
      engine.getUrlInMain()?.also { webViewUrl ->
        val newKey = "$webViewUrl - $faviconHref"
        /// 如果已经成功通过 onReceivedIcon 获取到 icon，那么这里就不再进行 fetch
        if (newKey != cacheKey) {
          cacheKey = newKey
          stateFlow.value = if (faviconHref.isNotEmpty()) {
            runCatching {
              defaultHttpPureClient.fetch(PureClientRequest(faviconHref, method = PureMethod.GET))
                .binary().toImageBitmap()
            }.getOrNull()
          } else null
        }
      }
    }

    engine.dWebChromeClient.addWebChromeClient(object : WebChromeClient() {
      override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        cacheKey = "${view?.url} - ${engine.dwebFavicon.urlFlow.value}"
        stateFlow.value = icon?.asImageBitmap()
      }
    })
  }