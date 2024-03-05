package org.dweb_browser.dwebview.messagePort

import com.teamdev.jxbrowser.js.JsObject
import org.dweb_browser.dwebview.DWebMessage
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.IWebMessagePort
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.launchWithMain
import org.dweb_browser.helper.withMainContext

class DWebMessagePort(val port: /* MessagePort */JsObject, private val webview: DWebView) : IWebMessagePort {
  internal val _started = lazy {
    val onMessageSignal = Signal<DWebMessage>()
    webview.ioScope.launchWithMain {
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
