package org.dweb_browser.dwebview.ipcWeb

import android.webkit.WebMessage
import android.webkit.WebMessagePort
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.MicroModuleManifest
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IPC_ROLE
import org.dweb_browser.microservice.ipc.helper.IpcMessage
import org.dweb_browser.microservice.ipc.helper.IpcMessageArgs
import org.dweb_browser.microservice.ipc.helper.IpcRequest
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.ipc.helper.IpcStreamData
import org.dweb_browser.microservice.ipc.helper.jsonToIpcMessage
import java.util.WeakHashMap

fun debugMessagePortIpc(tag: String, msg: Any = "", err: Throwable? = null) =
  printdebugln("message-port-ipc", tag, msg, err)

class MessagePort private constructor(private val port: WebMessagePort) {
  companion object {
    private val wm = WeakHashMap<WebMessagePort, MessagePort>()
    fun from(port: WebMessagePort): MessagePort = wm.getOrPut(port) { MessagePort(port) }
    val messageScope = CoroutineScope(CoroutineName("webMessage") + ioAsyncExceptionHandler)
  }

  val messageChannel = Channel<WebMessage>(capacity = Channel.UNLIMITED)

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

    port.setWebMessageCallback(object : WebMessagePort.WebMessageCallback() {
      override fun onMessage(port: WebMessagePort, event: WebMessage) {
        messageChannel.trySend(event).getOrElse { err ->
          err?.printStackTrace()
        }
        /// TODO 尝试告知对方暂停，比如发送 StreamPaused
      }
    })

    signal
  }

  fun onWebMessage(cb: Callback<WebMessage>) = _messageSignal.listen(cb)
  fun postMessage(data: String) = port.postMessage(WebMessage(data))

  private var _isClosed = false
  fun close() {
    if (_isClosed) {
      return
    }
    _isClosed = true
    messageChannel.close()
    port.close()
  }
}

open class MessagePortIpc(
  val port: MessagePort,
  override val remote: MicroModuleManifest,
  private val roleType: IPC_ROLE,
) : Ipc() {
  constructor(
    port: WebMessagePort, remote: MicroModuleManifest, roleType: IPC_ROLE
  ) : this(MessagePort.from(port), remote, roleType)

  override val role get() = roleType.role
  override fun toString(): String {
    return super.toString() + "@MessagePortIpc(${remote.mmid}, $roleType)"
  }

  init {
    val callback = port.onWebMessage { event ->
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
    }
    onDestroy(callback)

  }

  override suspend fun _doPostMessage(data: IpcMessage) {
    val message = when (data) {
      is IpcRequest -> gson.toJson(data.ipcReqMessage)
      is IpcResponse -> gson.toJson(data.ipcResMessage)
      is IpcStreamData -> gson.toJson(data)
      else -> gson.toJson(data)
    }
    this.port.postMessage(message)
  }

  override suspend fun _doClose() {
    this.port.postMessage("close")
    this.port.close()
  }

   suspend fun emitClose() {
    closeSignal.emit()
  }
}

