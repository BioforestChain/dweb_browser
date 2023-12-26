package org.dweb_browser.pure.image

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.pure.image.offscreenwebcanvas.OffscreenWebCanvasCore

actual class OffscreenWebCanvas private actual constructor(width: Int, height: Int) {
  companion object {
    init {
      WebView.setWebContentsDebuggingEnabled(true)
    }
  }

  private lateinit var webview: WebView

  @SuppressLint("SetJavaScriptEnabled")
  constructor(context: Context, width: Int = 128, height: Int = 128) : this(width, height) {
    webview = WebView(context)
    CoroutineScope(mainAsyncExceptionHandler).launch {
      webview.webViewClient = WebViewClient();
      webview.settings.javaScriptEnabled = true;
      webview.loadUrl(core.channel.getEntryUrl(width, height))
    }
  }

  internal actual val core = OffscreenWebCanvasCore()

}