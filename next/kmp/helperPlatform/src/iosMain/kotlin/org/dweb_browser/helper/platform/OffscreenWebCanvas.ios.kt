package org.dweb_browser.helper.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.offscreenwebcanvas.OffscreenWebCanvasCore
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL.Companion.URLWithString
import platform.Foundation.NSURLRequest.Companion.requestWithURL
import platform.Foundation.setValue
import platform.UIKit.UIDevice
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.javaScriptEnabled

actual class OffscreenWebCanvas private actual constructor(width: Int, height: Int) {
  internal actual val core = OffscreenWebCanvasCore()
  private lateinit var webview: WKWebView

  @OptIn(ExperimentalForeignApi::class)
  constructor(
    config: WKWebViewConfiguration = WKWebViewConfiguration(),
    width: Int = 128,
    height: Int = 128,
  ) : this(width, height) {
    config.preferences.javaScriptEnabled = true
    this.webview = WKWebView(frame = cValue { CGRectZero }, configuration = config).also {
      if(UIDevice.currentDevice.systemVersion.compareTo("16.4", true) >= 0) {
        it.setInspectable(true)
      }

      CoroutineScope(mainAsyncExceptionHandler).launch {
        it.loadRequest(requestWithURL(URLWithString(core.channel.getEntryUrl(width, height))!!))
      }
    }
  }


}