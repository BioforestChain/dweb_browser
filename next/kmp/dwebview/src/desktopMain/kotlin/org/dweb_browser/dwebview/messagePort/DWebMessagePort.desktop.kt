package org.dweb_browser.dwebview.messagePort

import com.teamdev.jxbrowser.js.JsArray
import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebMessage
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.IWebMessagePort
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.launchWithMain
import org.dweb_browser.helper.runIf
import kotlin.jvm.optionals.getOrNull

class DWebMessagePort(val port: /* MessagePort */JsObject, private val webview: DWebView) :
  IWebMessagePort {
  internal val _started = lazy {
    val onMessageSignal = Signal<DWebMessage>()
    webview.ioScope.launch {
      port.call<Unit>("addEventListener", "message", JsFunctionCallback {
        (it[0] as JsObject).apply {
          val ports = property<JsArray>("ports").runIf { jsPorts ->
            mutableListOf<DWebMessagePort>().apply {
              for (index in 0..<jsPorts.length()) {
                add(DWebMessagePort(jsPorts.get<JsObject>(index)!!, webview))
              }
            }
          } ?: emptyList()

          when (val message = property<Any>("data").getOrNull()) {
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
      port.call<Unit>("start")
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
    when (event) {
      is DWebMessage.DWebMessageBytes -> {
        val ports = event.ports.map {
          require(it is DWebMessagePort)
          it.port
        }

        port.call<Unit>("postMessage", event.data, ports)
      }

      is DWebMessage.DWebMessageString -> {
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
