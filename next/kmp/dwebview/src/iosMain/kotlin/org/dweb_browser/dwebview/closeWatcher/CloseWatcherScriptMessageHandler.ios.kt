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
    didReceiveScriptMessage: WKScriptMessage,
  ) {
    val message = didReceiveScriptMessage.body as NSObject
    if ("init" == message.valueForKey("cmd")) {
      engine.closeWatcher.reset()
    }

    (message.valueForKey("token") as String?)?.also { consumeToken ->
      if (consumeToken.isNotEmpty()) {
        engine.mainScope.launch {
          engine.closeWatcher.registryToken(consumeToken)
        }
      }
    }
    (message.valueForKey("id") as String?)?.also { closeId ->
      if (closeId.isNotEmpty()) {
        engine.closeWatcher.tryClose(closeId)
      }
    }

    (message.valueForKey("destroy") as String?)?.also { destroyId ->
      if (destroyId.isNotEmpty()) {
        engine.closeWatcher.tryDestroy(destroyId)
      }
    }
  }
}