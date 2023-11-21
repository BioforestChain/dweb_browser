package org.dweb_browser.dwebview

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.withMainContext
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.create

class DWebMessagePort(val portId: Int, private val webview: DWebView) : IWebMessagePort {
  init {
    DWebViewWebMessage.allPorts[portId] = this
  }

  internal val _started = lazy {
    val onMessageSignal = Signal<DWebMessage>()
    webview.scope.launch {
      withMainContext {
        webview.engine.evalAsyncJavascript<Unit>(
          "nativeStart($portId)",
          null,
          DWebViewWebMessage.webMessagePortContentWorld
        ).await()
      }
    }
    onMessageSignal
  }

  override suspend fun start() {
    _started.value
  }

  @OptIn(BetaInteropApi::class)
  override suspend fun close() {
    withMainContext {
      val arguments = mutableMapOf<NSString, NSNumber>().apply {
        put(NSString.create(string = "portId"), NSNumber(portId))
      }

      webview.engine.callAsyncJavaScript<Unit>(
        "nativeClose(portId)",
        arguments.toMap(),
        null,
        DWebViewWebMessage.webMessagePortContentWorld
      )
    }
  }

  override suspend fun postMessage(event: DWebMessage) {
    withMainContext {
      val ports = event.ports.map {
        require(it is DWebMessagePort)
        it.portId
      }.joinToString(",")
      webview.engine.evalAsyncJavascript<Unit>(
        "nativePortPostMessage($portId, ${
          Json.encodeToString(
            event.data
          )
        }, [$ports])", null, DWebViewWebMessage.webMessagePortContentWorld
      ).await()
    }
  }

  override val onMessage = _started.value.toListener()
}
