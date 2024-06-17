package org.dweb_browser.dwebview.polyfill

import platform.WebKit.WKContentWorld
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.darwin.NSObject

object FaviconPolyfill {
  val faviconContentWorld = WKContentWorld.worldWithName("favicon");
}

class DWebViewFaviconMessageHandler(val onChange:(String)->Unit) : NSObject(),
  WKScriptMessageHandlerProtocol {
  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage
  ) {
    try {
      val iconHref = didReceiveScriptMessage.body as String
      onChange(iconHref)
    } catch (_: Throwable) {
    }
  }
}