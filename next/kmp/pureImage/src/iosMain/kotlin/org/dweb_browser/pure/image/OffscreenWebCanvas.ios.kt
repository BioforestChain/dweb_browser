package org.dweb_browser.pure.image

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.helper.globalMainScope
import org.dweb_browser.platform.ios.DwebWKWebView
import org.dweb_browser.pure.image.offscreenwebcanvas.OffscreenWebCanvasCore
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL.Companion.URLWithString
import platform.Foundation.NSURLRequest.Companion.requestWithURL
import platform.UIKit.UIDevice
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.javaScriptEnabled

actual class OffscreenWebCanvas private actual constructor(width: Int, height: Int) {
  companion object {
    val defaultInstance by lazy { OffscreenWebCanvas(128, 128) }
  }

  internal actual val core = OffscreenWebCanvasCore()

  @OptIn(ExperimentalForeignApi::class)
  val webview = DwebWKWebView(
    // 不可为CGRectZero，否则会被直接回收
    frame = CGRectMake(x = .0, y = .0, width = 10.0, height = 10.0),
    configuration = WKWebViewConfiguration().apply { preferences.javaScriptEnabled },
  ).also { webview ->
    if (UIDevice.currentDevice.systemVersion.compareTo("16.4", true) >= 0) {
      webview.setInspectable(true)
    }
    globalMainScope.launch {
      core.channel.entryUrlFlow.collect { url ->
        webview.loadRequest(requestWithURL(URLWithString(url)!!))
      }
    }
  }
}
