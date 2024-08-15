package org.dweb_browser.dwebview.engine

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.dwebview.WebLoadErrorState
import org.dweb_browser.dwebview.WebLoadStartState
import org.dweb_browser.dwebview.WebLoadSuccessState

fun setupLoadStateFlow(engine: DWebViewEngine, initUrl: String) = MutableStateFlow(
  when (initUrl) {
    "", "about:blank" -> WebLoadSuccessState("about:blank")
    else -> WebLoadStartState(initUrl)
  }
).also { flow ->
  engine.addWebViewClient(object : WebViewClient() {
    override fun onPageFinished(view: WebView, url: String?) {
      flow.value = WebLoadSuccessState(url ?: "about:blank")
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
      flow.value = WebLoadStartState(url ?: "about:blank")
    }

    override fun onReceivedError(
      view: WebView, request: WebResourceRequest?, error: WebResourceError?,
    ) {
      // url必须相等，否则一些网页内资源异常会导致下一个网页无法正常加载
      if (request?.url?.toString()?.trimEnd('/') == view.url?.trimEnd('/')) {
        flow.value = WebLoadErrorState(view.url ?: "about:blank",
          error?.let { "[${it.errorCode}]${it.description}" } ?: "")
      }
    }
  })
}