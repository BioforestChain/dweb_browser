package org.dweb_browser.dwebview.messagePort

import com.teamdev.jxbrowser.js.JsArray
import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.launchWithMain
import org.dweb_browser.helper.runIf
import kotlin.jvm.optionals.getOrNull

class DWebMessagePort(val port: /* MessagePort */JsObject, private val webview: DWebView) :
  IWebMessagePort {
  private val _started = lazy {
    val messageChannel = Channel<DWebMessage>(capacity = Channel.UNLIMITED)

    val cb = JsFunctionCallback {
      (it[0] as JsObject).apply {
        val ports = property<JsArray>("ports").runIf { jsPorts ->
          mutableListOf<DWebMessagePort>().apply {
            for (index in 0..<jsPorts.length()) {
              add(DWebMessagePort(jsPorts.get<JsObject>(index)!!, webview))
            }
          }
        } ?: emptyList()

        val message = property<Any>("data").getOrNull()
        if (message is String) {
          debugDWebView(
            "message-in",
            when (val len = message.length) {
              in 0..100 -> message
              else -> message.slice(0..59) + "..." + message.slice(len - 50..<len)
            }
          )
        }
        when (message) {
          is String -> DWebMessage.DWebMessageString(message, ports)
          is ByteArray -> DWebMessage.DWebMessageBytes(message, ports)
          else -> null
        }?.also { dwebMessage ->
          messageChannel.trySend(dwebMessage)
        }

        // release jsObject
        close()
      }
    }
    port.call<Unit>("addEventListener", "message", cb)
    port.call<Unit>("start")
    messageChannel.invokeOnClose {
      port.call<Unit>("removeEventListener", "message", cb)
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
    _started.value
  }
}
