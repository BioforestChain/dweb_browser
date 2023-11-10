package org.dweb_browser.dwebview

import kotlinx.cinterop.BetaInteropApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.withMainContext
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.create

class DWebMessagePort(val portId: Int, private val webview: DWebView) : IWebMessagePort {
  private val onMessageSignal = Signal<DWebMessage>()

  init {
    DWebViewWebMessage.allPorts[portId] = this
  }

  override suspend fun start() {
    withMainContext {
      webview.evalAsyncJavascript<Unit>(
        "nativeStart($portId)",
        null,
        DWebViewWebMessage.webMessagePortContentWorld
      ).await()
    }
  }

  @OptIn(BetaInteropApi::class)
  override suspend fun close() {
    withMainContext {
      val arguments = mutableMapOf<NSString, NSNumber>().apply {
        put(NSString.create(string = "portId"), NSNumber(portId))
      }

      webview.callAsyncJavaScript<Unit>(
        "nativeClose(portId)",
        arguments.toMap(),
        null,
        DWebViewWebMessage.webMessagePortContentWorld
      ).await()
    }
  }

  override suspend fun postMessage(event: DWebMessage) {
    withMainContext {
      val ports = event.ports.map {
        require(it is DWebMessagePort)
        it.portId
      }.joinToString(",")
      webview.evalAsyncJavascript<Unit>(
        "nativePortPostMessage($portId, ${
          Json.encodeToString(
            event.data
          )
        }, [$ports])", null, DWebViewWebMessage.webMessagePortContentWorld
      ).await()
    }
  }

  override val onMessage = onMessageSignal.toListener()
}
