package org.dweb_browser.dwebview

import org.dweb_browser.dwebview.engine.DWebViewEngine
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.darwin.NSObject

class DWebViewWatchIconHandler(val engine: DWebViewEngine) : NSObject(), WKScriptMessageHandlerProtocol {
  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage
  ) {
    try {
      val message = didReceiveScriptMessage.body as String
      engine.setIcon(message)
    } catch(_: Throwable) {}
  }
}