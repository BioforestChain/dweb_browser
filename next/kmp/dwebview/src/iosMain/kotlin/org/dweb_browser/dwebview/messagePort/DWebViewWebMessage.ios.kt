package org.dweb_browser.dwebview.messagePort

import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.dwebview.DWebView
import platform.Foundation.NSNumber
import platform.Foundation.valueForKey
import platform.WebKit.WKContentWorld
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.darwin.NSObject

internal class DWebViewWebMessage(val webview: DWebView) {
  companion object {
    val webMessagePortContentWorld = WKContentWorld.worldWithName("web-message-port");
    val allPorts = mutableMapOf<Int, DWebMessagePort>()
  }

  internal class WebMessagePortMessageHandler : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
      userContentController: WKUserContentController,
      didReceiveScriptMessage: WKScriptMessage,
    ) {
      try {
        val message = didReceiveScriptMessage.body as NSObject
        val type = message.valueForKey("type") as String

        if (type == "message") {
          val id = (message.valueForKey("id") as NSNumber).intValue
          val data = message.valueForKey("data") as String
          val ports = mutableListOf<DWebMessagePort>()
          val originPort = allPorts[id] ?: throw Exception("no found port by id:$id")

          originPort.dispatchMessage(DWebMessage.DWebMessageString(data, ports))
        }
      } catch (_: Throwable) {
      }
    }
  }
}