package org.dweb_browser.dwebview.closeWatcher

import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.engine.DWebViewEngine
import platform.Foundation.valueForKey
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.darwin.NSObject

internal class CloseWatcherScriptMessageHandler(private val engine: DWebViewEngine) : NSObject(),
  WKScriptMessageHandlerProtocol {
  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage
  ) {
    val message = didReceiveScriptMessage.body as NSObject
    val consumeToken = message.valueForKey("token") as String
    val id = message.valueForKey("id") as String

    if (consumeToken.isNotEmpty()) {
      engine.mainScope.launch {
        engine.closeWatcher.registryToken(consumeToken)
      }
    } else if (id.isNotEmpty()) {
      engine.closeWatcher.tryClose(id)
    }
  }
}