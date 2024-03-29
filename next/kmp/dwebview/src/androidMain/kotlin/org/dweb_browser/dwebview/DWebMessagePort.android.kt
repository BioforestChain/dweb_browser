package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebMessagePortCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.defaultAsyncExceptionHandler

@SuppressLint("RestrictedApi")
class DWebMessagePort private constructor(internal val port: WebMessagePortCompat) :
  IWebMessagePort {
  companion object {
    private val wm = WeakHashMap<WebMessagePortCompat, DWebMessagePort>()
    fun from(port: WebMessagePortCompat): DWebMessagePort =
      wm.getOrPut(port) { DWebMessagePort(port) }

    fun IWebMessagePort.into(): WebMessagePortCompat {
      require(this is DWebMessagePort)
      return port
    }
  }

  @SuppressLint("RequiresFeature")
  private val _started = lazy {
    val messageChannel = Channel<DWebMessage>(capacity = Channel.UNLIMITED)

    port.setWebMessageCallback(object : WebMessagePortCompat.WebMessageCallbackCompat() {
      override fun onMessage(port: WebMessagePortCompat, message: WebMessageCompat?) {
        if (message?.type == WebMessageCompat.TYPE_ARRAY_BUFFER) {
          if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER)) {
            messageChannel.trySend(
              DWebMessage.DWebMessageBytes(
                message.arrayBuffer,
                message.ports?.map { from(it) } ?: emptyList()))
            return
          }
        }

        messageChannel.trySend(
          DWebMessage.DWebMessageString(
            message?.data ?: "",
            message?.ports?.map { from(it) } ?: emptyList())
        ).getOrElse { err ->
          err?.printStackTrace()
        }
      }
    })
    val messageScope = CoroutineScope(CoroutineName("webMessage") + defaultAsyncExceptionHandler)
    val onMessageSignal = Signal<DWebMessage>()
    messageScope.launch {
      /// 这里为了确保消息的顺序正确性，比如使用channel来一帧一帧地读取数据，不可以直接用 launch 去异步执行 event，这会导致下层解析数据的顺序问题
      /// 并发性需要到消息被解码出来后才能去执行并发。也就是非 IpcStream 类型的数据才可以走并发
      for (event in messageChannel) {
        onMessageSignal.emit(event)
      }
      onMessageSignal.clear()
    }
    onMessageSignal
  }

  override suspend fun start() {
    _started.value
  }

  @SuppressLint("RequiresFeature")
  override suspend fun close() {
    port.close()
  }

  override val onMessage get() = _started.value.toListener()

  @SuppressLint("RequiresFeature")
  override suspend fun postMessage(event: DWebMessage) {
    val ports = if (event.ports.isEmpty()) null
    else event.ports.map { (it as DWebMessagePort).port }.toTypedArray()

    val msgCompat = when (event) {
      is DWebMessage.DWebMessageBytes -> {
        WebMessageCompat(event.data, ports)
      }

      is DWebMessage.DWebMessageString -> {
        WebMessageCompat(event.data, ports)
      }
    }
    port.postMessage(msgCompat)
  }
}