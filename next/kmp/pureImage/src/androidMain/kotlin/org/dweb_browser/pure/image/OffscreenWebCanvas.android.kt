package org.dweb_browser.pure.image

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.launch
import org.dweb_browser.helper.globalMainScope
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
    globalMainScope.launch {
      webview.webViewClient = WebViewClient();
      webview.settings.javaScriptEnabled = true;
      core.channel.entryUrlFlow.collect {
        webview.loadUrl(it)
      }
    }
  }

  internal actual val core = OffscreenWebCanvasCore()

}