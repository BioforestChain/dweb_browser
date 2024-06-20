package org.dweb_browser.dwebview.engine

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.dwebview.WebLoadState
import org.dweb_browser.dwebview.WebLoadSuccessState

fun setupLoadStateChangeFlow(engine: DWebViewEngine) = MutableStateFlow<WebLoadState>(
  WebLoadSuccessState(engine.url ?: "about:blank")
).also { stateFlow ->
  engine.addWebViewClient(object : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
      super.onPageStarted(view, url, favicon)
//      signal.value = WebLoadState.Loading
    }
  })
}