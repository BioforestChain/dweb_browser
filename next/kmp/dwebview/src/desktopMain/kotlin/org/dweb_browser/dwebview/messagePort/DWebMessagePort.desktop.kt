package org.dweb_browser.dwebview.messagePort

import com.teamdev.jxbrowser.js.JsArray
import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.helper.launchWithMain
import org.dweb_browser.helper.runIf
import kotlin.jvm.optionals.getOrNull

class DWebMessagePort(val port: /* MessagePort */JsObject, private val webview: DWebView) :
  IWebMessagePort {
  val scope = webview.ioScope + SupervisorJob()
  internal val _started = lazy {
    val messageChannel = Channel<DWebMessage>()

    webview.ioScope.launch {
      val cb = JsFunctionCallback {
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
            messageChannel.trySend(dwebMessage)
          }
        }
      }
      port.call<Unit>("addEventListener", "message", cb)
      port.call<Unit>("start")
      messageChannel.invokeOnClose {
        port.call<Unit>("removeEventListener", "message", cb)
      }
    }
    messageChannel
  }

  override suspend fun start() {
    _started.value
  }

  override suspend fun close(cause: CancellationException?) {
    if (_started.isInitialized()) {
      _started.value.close(cause)
    }
    webview.ioScope.launchWithMain {
      port.call<Unit>("close")
    }.join()
    scope.cancel(cause)
  }

  override suspend fun postMessage(event: DWebMessage): Unit = runCatching {
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
  }.getOrElse {
    /// jsObject 可能已经被释放了

  }

  override val onMessage by lazy {
    _started.value.consumeAsFlow().shareIn(scope, SharingStarted.Lazily)
  }
}
