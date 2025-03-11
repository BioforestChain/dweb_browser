package org.dweb_browser.dwebview.messagePort

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.helper.hexString
import org.dweb_browser.helper.launchWithMain
import org.dweb_browser.helper.withMainContext
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.create

class DWebMessagePort(val portId: Int, private val webview: DWebView, parentScope: CoroutineScope) :
  IWebMessagePort {
  init {
    DWebViewWebMessage.allPorts[portId] = this
  }

  internal val _started = lazy {
    val channel = Channel<DWebMessage>(capacity = Channel.UNLIMITED)
    webview.lifecycleScope.launchWithMain {
      webview.engine.evalAsyncJavascript<Unit>(
        "nativeStart($portId)", null, DWebViewWebMessage.webMessagePortContentWorld
      ).await()
    }
    channel
  }

  override suspend fun start() {
    _started.value
  }

  @OptIn(BetaInteropApi::class)
  override suspend fun close(cause: CancellationException?) {
    if (_started.isInitialized()) {
      _started.value.close(cause)
    }
    withMainContext {
      val arguments = mutableMapOf<NSString, NSNumber>().apply {
        put(NSString.create(string = "portId"), NSNumber(portId))
      }

      webview.engine.awaitAsyncJavaScript<Unit>(
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
      if (event is DWebMessage.DWebMessageBytes) {
        webview.engine.evalAsyncJavascript<Unit>(
          "nativePortPostMessage($portId, str_to_hex_binary(\"${
            event.binary.hexString
          }\"), [$ports])", null, DWebViewWebMessage.webMessagePortContentWorld
        ).await()
      } else if (event is DWebMessage.DWebMessageString) {
        webview.engine.evalAsyncJavascript<Unit>(
          "nativePortPostMessage($portId, ${
            Json.encodeToString(event.text)
          }, [$ports])", null, DWebViewWebMessage.webMessagePortContentWorld
        ).await()
      }
    }
  }

  internal fun dispatchMessage(message: DWebMessage) = _started.value.trySend(message)
  override val onMessage get() = _started.value
}
