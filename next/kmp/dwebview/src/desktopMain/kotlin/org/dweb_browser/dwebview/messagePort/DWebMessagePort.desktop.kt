package org.dweb_browser.dwebview.messagePort

import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebMessage
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.IWebMessagePort
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.launchWithMain
import org.dweb_browser.helper.runIf
import org.dweb_browser.helper.withMainContext
import kotlin.jvm.optionals.getOrNull

class DWebMessagePort(val port: /* MessagePort */JsObject, private val webview: DWebView) :
  IWebMessagePort {
  internal val _started = lazy {
    val onMessageSignal = Signal<DWebMessage>()
    webview.ioScope.launch {
      port.call<Unit>("start")
      port.call<Unit>("addEventListener", JsFunctionCallback {
        (it[0] as JsObject).apply {
          val ports = property<JsObject>("ports").runIf { jsPorts ->
            jsPorts.property<Number>("size").runIf { size ->
              mutableListOf<DWebMessagePort>().apply {
                for (index in 0..<size.toInt()) {
                  add(DWebMessagePort(jsPorts.property<JsObject>(index.toString()).get(), webview))
                }
              }
            }
          } ?: emptyList()

          when (val message = property<Any>("message").getOrNull()) {
            is String -> DWebMessage.DWebMessageString(message, ports)
            is ByteArray -> DWebMessage.DWebMessageBytes(message, ports)
            else -> null
          }?.also { dwebMessage ->
            webview.ioScope.launch {
              onMessageSignal.emit(dwebMessage)
            }
          }
        }
      })
    }
    onMessageSignal
  }

  override suspend fun start() {
    _started.value
  }

  override suspend fun close() {
    webview.ioScope.launchWithMain {
      port.call<Unit>("close")
    }
  }

  override suspend fun postMessage(event: DWebMessage) {
    withMainContext {
      if (event is DWebMessage.DWebMessageBytes) {
        val ports = event.ports.map {
          require(it is DWebMessagePort)
          it.port
        }

        port.call<Unit>("postMessage", event.data, ports)
      } else if (event is DWebMessage.DWebMessageString) {
        val ports = event.ports.map {
          require(it is DWebMessagePort)
          it.port
        }

        port.call<Unit>("postMessage", event.data, ports)
      }
    }
  }

  override val onMessage = _started.value.toListener()
}
