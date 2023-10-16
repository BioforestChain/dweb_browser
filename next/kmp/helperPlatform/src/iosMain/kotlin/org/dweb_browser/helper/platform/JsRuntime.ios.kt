package org.dweb_browser.helper.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.jsruntime.JsRuntimeCore
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL.Companion.URLWithString
import platform.Foundation.NSURLRequest.Companion.requestWithURL
import platform.Foundation.setValue
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.javaScriptEnabled

actual class JsRuntime private actual constructor() {
  actual val core = JsRuntimeCore()
  private lateinit var webview: WKWebView

  @OptIn(ExperimentalForeignApi::class)
  constructor(
    config: WKWebViewConfiguration = WKWebViewConfiguration(),
  ) : this() {
    config.preferences.javaScriptEnabled = true
    this.webview = WKWebView(frame = cValue { CGRectZero }, configuration = config).also {
//      if(UIDevice.currentDevice.systemVersion)
      it.setValue(value = true, forKey = "inspectable")
      CoroutineScope(mainAsyncExceptionHandler).launch {
        it.loadRequest(requestWithURL(URLWithString(core.ws.getEntryUrl())!!))
      }
    }
  }

}