package org.dweb_browser.helper.platform

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.jsruntime.JsRuntimeCore

actual class JsRuntime private actual constructor() {
  private lateinit var webview: WebView

  @SuppressLint("SetJavaScriptEnabled")
  constructor(context: Context) : this() {
    webview = WebView(context)
    CoroutineScope(mainAsyncExceptionHandler).launch {
      webview.webViewClient = WebViewClient();
      webview.settings.javaScriptEnabled = true;
      webview.loadUrl(core.ws.getEntryUrl())
    }
  }

  actual val core = JsRuntimeCore()
}