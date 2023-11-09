package org.dweb_browser.dwebview

import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebMessagePort.WebMessageCallback
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.defaultAsyncExceptionHandler

class DWebMessagePort private constructor(private val port: WebMessagePort) : IWebMessagePort {
  companion object {
    private val wm = WeakHashMap<WebMessagePort, DWebMessagePort>()
    fun from(port: WebMessagePort) = wm.getOrPut(port) { DWebMessagePort(port) }
  }

  init {
    port.setWebMessageCallback(object : WebMessageCallback() {
      override fun onMessage(port: WebMessagePort?, message: WebMessage?) {
        super.onMessage(port, message)
      }
    })
  }

  private val _started = lazy {
    val messageChannel = Channel<MessageEvent>(capacity = Channel.UNLIMITED)
    port.setWebMessageCallback(object : WebMessageCallback() {
      override fun onMessage(port: WebMessagePort?, message: WebMessage) {
        messageChannel.trySend(
          MessageEvent(
            message.data,
            message.ports?.map { from(it) } ?: emptyList()
          )
        ).getOrElse { err ->
          err?.printStackTrace()
        }
      }
    })
    val messageScope = CoroutineScope(CoroutineName("webMessage") + defaultAsyncExceptionHandler)
    val onMessageSignal = Signal<IMessageEvent>()
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

  override suspend fun close() {
    port.close()
  }

  private val onMessageSignal by _started
  override val onMessage = onMessageSignal.toListener()
  override suspend fun postMessage(event: IMessageEvent) {
    require(event is MessageEvent)
    port.postMessage(
      WebMessage(
        event.data,
        event.ports.map { (it as DWebMessagePort).port }.toTypedArray()
      )
    )
  }
}