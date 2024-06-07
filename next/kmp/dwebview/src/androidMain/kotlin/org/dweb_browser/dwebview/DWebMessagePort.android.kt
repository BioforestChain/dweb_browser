package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebMessagePortCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.globalMainScope
import org.dweb_browser.helper.withMainContext

@SuppressLint("RestrictedApi")
class DWebMessagePort private constructor(
  internal val port: WebMessagePortCompat,
  val engine: DWebViewEngine,
) : IWebMessagePort {
  companion object {
    private val wm = WeakHashMap<WebMessagePortCompat, DWebMessagePort>()
    fun from(port: WebMessagePortCompat, engine: DWebViewEngine): DWebMessagePort =
      wm.getOrPut(port) { DWebMessagePort(port, engine) }

    fun IWebMessagePort.into(): WebMessagePortCompat {
      require(this is DWebMessagePort)
      return port
    }
  }

  @SuppressLint("RequiresFeature")
  private val _started = lazy {
    val messageChannel = Channel<DWebMessage>(Channel.UNLIMITED)
    globalMainScope.launch {
      port.setWebMessageCallback(object : WebMessagePortCompat.WebMessageCallbackCompat() {
        override fun onMessage(port: WebMessagePortCompat, message: WebMessageCompat?) {
          message ?: return
          val dWebMessage = when (message.type) {
            WebMessageCompat.TYPE_STRING -> DWebMessage.DWebMessageString(message.data ?: "",
              message.ports?.map { from(it, engine) } ?: emptyList())

            WebMessageCompat.TYPE_ARRAY_BUFFER -> DWebMessage.DWebMessageBytes(message.arrayBuffer,
              message.ports?.map { from(it, engine) } ?: emptyList())

            else -> return
          }
          messageChannel.trySend(dWebMessage).getOrElse { err -> err?.printStackTrace() }
        }
      })
    }

    if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_PORT_CLOSE)) {
      messageChannel.invokeOnClose {
        port.close()
      }
    }
    messageChannel
  }

  override suspend fun start() {
    _started.value
  }

  @SuppressLint("RequiresFeature")
  override suspend fun close(cause: CancellationException?) {
    if (_started.isInitialized()) {
      _started.value.close(cause)
    }
    port.close()
  }

  override val onMessage by lazy {
    _started.value
  }

  @SuppressLint("RequiresFeature")
  override suspend fun postMessage(event: DWebMessage) {
    val ports = if (event.ports.isEmpty()) null
    else event.ports.map { (it as DWebMessagePort).port }.toTypedArray()

    val msgCompat = when (event) {
      is DWebMessage.DWebMessageBytes -> {
        WebMessageCompat(event.binary, ports)
      }

      is DWebMessage.DWebMessageString -> {
        WebMessageCompat(event.text, ports)
      }
    }
    withMainContext {
      try {
        port.postMessage(msgCompat)
      } catch (e: java.lang.Exception) {
        WARNING("post-close: ${e.message}")
      }
    }
  }
}