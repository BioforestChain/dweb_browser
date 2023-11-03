package org.dweb_browser.dwebview.ipcWeb

import android.webkit.WebMessage
import android.webkit.WebMessagePort
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.IPC_ROLE
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcMessageArgs
import org.dweb_browser.core.ipc.helper.jsonToIpcMessage
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.withMainContext

class MessagePort private constructor(private val port: WebMessagePort) : IMessagePort {
  companion object {
    private val wm = WeakHashMap<WebMessagePort, MessagePort>()
    suspend fun from(port: WebMessagePort): MessagePort =
      wm.getOrPut(port) { MessagePort(port).also { it.installMessageSignal() } }

    val messageScope = CoroutineScope(CoroutineName("webMessage") + ioAsyncExceptionHandler)
  }

  val messageChannel = Channel<WebMessage>(capacity = Channel.UNLIMITED)

  private suspend fun installMessageSignal() = withMainContext {
    port.setWebMessageCallback(object : WebMessagePort.WebMessageCallback() {
      override fun onMessage(port: WebMessagePort, event: WebMessage) {
        messageChannel.trySend(event).getOrElse { err ->
          err?.printStackTrace()
        }
        /// TODO 尝试告知对方暂停，比如发送 StreamPaused
      }
    })
  }

  private val _messageSignal by lazy {
    val signal = Signal<WebMessage>()
    messageScope.launch {
      /// 这里为了确保消息的顺序正确性，比如使用channel来一帧一帧地读取数据，不可以直接用 launch 去异步执行 event，这会导致下层解析数据的顺序问题
      /// 并发性需要到消息被解码出来后才能去执行并发。也就是非 IpcStream 类型的数据才可以走并发
      for (event in messageChannel) {
        signal.emit(event)
      }
      signal.clear()
    }

    signal
  }

  fun onWebMessage(cb: Callback<WebMessage>) = _messageSignal.listen(cb)
  override suspend fun postMessage(data: String) = withMainContext {
    port.postMessage(WebMessage(data))
  }

  private var _isClosed = false
  override fun close() {
    if (_isClosed) {
      return
    }
    _isClosed = true
    messageChannel.close()
    port.close()
  }
}

open class MessagePortIpc(
  override val port: MessagePort,
  override val remote: IMicroModuleManifest,
  private val roleType: IPC_ROLE,
) : DMessagePortIpc(port, remote, roleType) {
  companion object {
    suspend fun from(
      port: WebMessagePort, remote: IMicroModuleManifest, roleType: IPC_ROLE
    ) = MessagePortIpc(MessagePort.from(port), remote, roleType)
  }

  init {
    port.onWebMessage { event ->
      val ipc = this@MessagePortIpc
      when (val message = jsonToIpcMessage(event.data, ipc)) {
        "close" -> close()
        "ping" -> port.postMessage("pong")
        "pong" -> debugMessagePortIpc("PONG", "$ipc")
        is IpcMessage -> {
          debugMessagePortIpc("ON-MESSAGE", "$ipc => $message")
          _messageSignal.emit(IpcMessageArgs(message, ipc))
        }

        else -> throw Exception("unknown message: $message")
      }
    }.removeWhen(onDestroy)

  }


}

