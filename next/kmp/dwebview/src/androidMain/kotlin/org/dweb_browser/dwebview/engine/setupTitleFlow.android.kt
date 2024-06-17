package org.dweb_browser.dwebview.engine

import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow

fun setupTitleFlow(engine: DWebViewEngine) = MutableStateFlow("").also { stateFlow ->
  engine.dWebChromeClient.addWebChromeClient(object : WebChromeClient() {
    override fun onReceivedTitle(view: WebView?, title: String?) {
      stateFlow.value = title ?: ""
    }
  })
}