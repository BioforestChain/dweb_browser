package org.dweb_browser.dwebview

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.withMainContext

class DWebMessagePort(val portId: Int, private val webview: DWebView) : IWebMessagePort {
  private val onMessageSignal = Signal<IMessageEvent>()
  override suspend fun start() {
    withMainContext {
      webview.evalAsyncJavascript<Unit>(
        "nativeStart($portId)",
        null,
        DWebViewWebMessage.webMessagePortContentWorld
      ).await()
    }
  }

  override suspend fun close() {
    withMainContext {
//        val a = NSString.create(string = "portId")
      val arguments = mutableMapOf<Array<String>, Array<Int>>().apply {
        put(arrayOf("portId"), arrayOf(portId))
      }

      webview.callAsyncJavaScript<Unit>(
        "nativeClose(portId)",
        arguments.toMap(),
        null,
        DWebViewWebMessage.webMessagePortContentWorld
      ).await()
    }
  }

  override suspend fun postMessage(event: IMessageEvent) {
    require(event is MessageEvent)
    withMainContext {
      val ports = event.ports.map {
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

data class MessageEvent(
  override val data: String,
  override val ports: List<DWebMessagePort> = emptyList()
) : IMessageEvent