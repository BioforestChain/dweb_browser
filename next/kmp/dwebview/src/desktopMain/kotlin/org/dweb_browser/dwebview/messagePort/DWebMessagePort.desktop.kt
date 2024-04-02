package org.dweb_browser.dwebview.messagePort

import com.teamdev.jxbrowser.js.JsArray
import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.launchWithMain
import org.dweb_browser.helper.runIf
import kotlin.jvm.optionals.getOrNull

class DWebMessagePort(val port: /* MessagePort */JsObject, private val webview: DWebView) :
  IWebMessagePort {
  val scope = webview.ioScope + SupervisorJob()
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

  override suspend fun close(cause: CancellationException?) {
    webview.ioScope.launchWithMain {
      port.call<Unit>("close")
    }.join()
    scope.cancel(cause)
  }

  override suspend fun postMessage(event: DWebMessage) {
    when (event) {
      is DWebMessage.DWebMessageBytes -> {
        val ports = event.ports.map {
          require(it is DWebMessagePort)
          it.port
        }

        port.call<Unit>("postMessage", event.binary, ports)
      }

      is DWebMessage.DWebMessageString -> {
        val ports = event.ports.map {
          require(it is DWebMessagePort)
          it.port
        }

        port.call<Unit>("postMessage", event.text, ports)
      }
    }
  }

  private val messageFlow = MutableSharedFlow<DWebMessage>()
  override val onMessage = messageFlow.shareIn(scope, SharingStarted.Lazily)
}
